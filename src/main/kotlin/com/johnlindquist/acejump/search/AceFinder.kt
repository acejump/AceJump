package com.johnlindquist.acejump.search

import com.google.common.collect.*
import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.util.EventDispatcher
import com.johnlindquist.acejump.keycommands.AceJumper
import com.johnlindquist.acejump.ui.JumpInfo
import java.util.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import kotlin.comparisons.compareBy

class AceFinder(val findManager: FindManager, val editor: EditorImpl) {
  var queryString: String = ""
  val document = editor.document as DocumentImpl
  val eventDispatcher = EventDispatcher.create(ChangeListener::class.java)
  val findModel: FindModel = findManager.findInFileModel.clone()
  var jumpLocations: Collection<JumpInfo> = emptyList()
  var tagMap: BiMap<String, Int> = HashBiMap.create()
  var unseen1grams: LinkedHashSet<String> = linkedSetOf()
  var unseen2grams: LinkedHashSet<String> = linkedSetOf()
  var adjacent = mapOf(
    'j' to "jikmnhu", 'f' to "ftgvcdr", 'k' to "kolmji", 'd' to "drfcxse",
    'l' to "lkop", 's' to "sedxzaw", 'a' to "aqwsz",
    'h' to "hujnbgy", 'g' to "gyhbvft", 'y' to "y7uhgt6", 't' to "t6ygfr5",
    'u' to "u8ijhy7", 'r' to "r5tfde4", 'n' to "nbhjm", 'v' to "vcfgb",
    'm' to "mnjk", 'c' to "cxdfv", 'b' to "bvghn",
    'i' to "i9okju8", 'e' to "e4rdsw3", 'x' to "xzsdc", 'z' to "zasx",
    'o' to "o0plki9", 'w' to "w3esaq2", 'p' to "plo0", 'q' to "q12wa"
    //'1' to "12q", '2' to "23wq1", '3' to "34ew2", '4' to "45re3",
    //'5' to "56tr4", '6' to "67yt5", '7' to "78uy6", '8' to "89iu7",
    //'9' to "90oi8", '0' to "0po9"
  )

  init {
    findModel.isFindAll = true
    findModel.isFromCursor = true
    findModel.isForward = true
    findModel.isRegularExpressions = false
    findModel.isWholeWordsOnly = false
    findModel.isCaseSensitive = false
    findModel.isPreserveCase = false
    findModel.setSearchHighlighters(true)
  }

  fun find(text: String, key: Char) {
    if (key == 0.toChar())
      reset()

    queryString = text
    findModel.stringToFind = text

    val application = ApplicationManager.getApplication()
    application.runReadAction(jump(key))
    application.invokeLater({
      if (text.isNotEmpty())
        eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
    })
  }

  var targetModeEnabled = false
  fun toggleTargetMode(): Boolean {
    targetModeEnabled = !targetModeEnabled
    return targetModeEnabled
  }

  val aceJumper = AceJumper(editor, document)
  private fun jump(key: Char): () -> Unit {
    fun jumpTo(jumpInfo: JumpInfo) {
      if (key.isUpperCase())
        aceJumper.setSelectionFromCaretToOffset(jumpInfo.offset)

      aceJumper.moveCaret(jumpInfo.offset)

      if (targetModeEnabled)
        aceJumper.selectWordAtCaret()

      reset()
    }

    return {
      jumpLocations = determineJumpLocations()
      if (jumpLocations.size <= 1) {
        val text = queryString.toLowerCase()
        if (tagMap.containsKey(text)) {
          jumpTo(JumpInfo(text, text, tagMap[text]!!, editor))
        } else if (2 <= text.length) {
          val last1: String = text.substring(text.length - 1)
          val last2: String = text.substring(text.length - 2)
          if (tagMap.containsKey(last2)) {
            jumpTo(JumpInfo(last2, text, tagMap[last2]!!, editor))
          } else if (tagMap.containsKey(last1)) {
            jumpTo(JumpInfo(last1, text, tagMap[last1]!!, editor))
          }
        }
      }
    }
  }

  private fun determineJumpLocations(): Collection<JumpInfo> {
    populateNgrams()
    val fullText = document.charsSequence.toString().toLowerCase()
    val sitesToCheck = getSitesInView(fullText)
    val existingDigraphs = makeMap(fullText, sitesToCheck)
    if (existingDigraphs.isEmpty)
      tagMap = filterTags(tagMap, queryString)
    else
      tagMap = compact(mapUniqueDigraphs(existingDigraphs))

    return plotJumpLocations()
  }

  fun populateNgrams() {
    val a_z = 'a'..'z'
    unseen1grams.addAll(a_z.mapTo(linkedSetOf(), { "$it" }))
    unseen1grams.addAll(('0'..'9').mapTo(linkedSetOf(), { "$it" }))
    a_z.flatMapTo(unseen2grams, { e -> a_z.map { c -> "${e}$c" } })
  }

  private fun filterTags(tags: BiMap<String, Int>, prefix: String) =
    tags.filterTo(HashBiMap.create(tags.size), { e ->
      prefix.endsWith(e.key) || prefix.endsWith(e.key[0])
    })

  fun compact(tagMap: BiMap<String, Int>) =
    tagMap.mapKeysTo(HashBiMap.create(tagMap.size), { e ->
      val firstCharacter = e.key[0].toString()
      if (tagMap.keys.count { it[0] == e.key[0] } == 1 &&
        unseen1grams.contains(firstCharacter) &&
        !queryString.endsWith(firstCharacter) &&
        !queryString.endsWith(e.key))
        firstCharacter
      else e.key
    })

  private fun getSitesInView(fullText: String): List<Int> {
    fun getSitesToCheck(window: String): Iterable<Int> {
      if (queryString.isEmpty())
        return 0..(window.length - 2)

      val indicesToCheck = arrayListOf<Int>()
      var result = findManager.findString(window, 0, findModel)
      while (result.isStringFound) {
        indicesToCheck.add(result.endOffset)
        result = findManager.findString(window, result.endOffset, findModel)
      }

      return indicesToCheck
    }

    val (startIndex, endIndex) = getVisibleRange(editor)
    val window = fullText.substring(startIndex, endIndex)
    return getSitesToCheck(window).map { it + startIndex }
  }

  fun makeMap(text: CharSequence, sites: Iterable<Int>): Multimap<String, Int> {
    val stringToIndex = LinkedListMultimap.create<String, Int>()
    for (site in sites) {
      var (p1, p2) = Pair(site, site + 1)
      var (c1, c2) = Pair(text[p1], text[p2])
      val origin = p1 - queryString.length
      stringToIndex.put("$c1", origin)
      stringToIndex.put("$c1$c2", origin)
      unseen1grams.remove("$c1")
      unseen2grams.removeAll { it.startsWith(c1) }

      while (c1.isLetterOrDigit() && c2.isLetterOrDigit()) {
        unseen2grams.remove("$c1$c2")
        p1++; p2++; c1 = text[p1]; c2 = text[p2]
      }
    }

    return stringToIndex
  }

  fun mapUniqueDigraphs(digraphs: Multimap<String, Int>): BiMap<String, Int> {
    val newTagMap: BiMap<String, Int> = HashBiMap.create()
    val unusedNgrams = LinkedHashSet<String>(unseen1grams)
    fun hasNearbyTag(index: Int): Boolean {
      val chars = document.charsSequence
      var (left, right) = Pair(index, index)
      while (0 <= left && chars[left].isLetterOrDigit()) {
        left--
      }
      while (chars.length < right && chars[right].isLetterOrDigit()) {
        right++
      }

      return (left..right).any { newTagMap.containsValue(it) }
    }

    fun tryToAssignTagToIndex(index: Int, tag: String) {
      if (hasNearbyTag(index) || newTagMap.containsValue(index))
        return

      val choosenTag = tagMap.inverse().getOrElse(index, { tag })
      newTagMap[choosenTag] = index
      if (choosenTag.length == 1) {
        unusedNgrams.removeAll { it[0] == choosenTag[0] }
      } else {
        unusedNgrams.remove(choosenTag[0].toString())
        unusedNgrams.remove(choosenTag)
      }
    }

    // Add pre-existing tags where search string and tag are intermingled
    for (entry in tagMap) {
      if (queryString == entry.key || queryString.last() == entry.key.first()) {
        newTagMap[entry.key] = entry.value
        unusedNgrams.remove(entry.key[0].toString())
      }
    }

    val remaining = digraphs.asMap().entries.sortedBy { it.value.size }
    val bigrams = unseen2grams.sortedWith(compareBy({
      !(adjacent.containsKey(it[0]) && adjacent[it[0]]!!.contains(it[1]))
    }, { it.last() })).iterator()
    var tagsNeeded = remaining.sumBy { it.value.size } - unseen1grams.size
    while (bigrams.hasNext() && 0 <= tagsNeeded--) {
      val biGram = bigrams.next()
      if (unusedNgrams.any { it[0].toString() == biGram[0].toString() })
        unusedNgrams.remove(biGram[0].toString())
      unusedNgrams.add(biGram)
    }

    val remainingSites = remaining.filter {
      it.key.first().isLetterOrDigit() || queryString.isNotEmpty()
    }.flatMap { it.value }.listIterator()

    while (remainingSites.hasNext() && unusedNgrams.isNotEmpty())
      tryToAssignTagToIndex(remainingSites.next(), unusedNgrams.first())

    return newTagMap
  }

  fun findPattern(text: Pattern) {
    findModel.isRegularExpressions = true
    find(text.pattern, 0.toChar())
    findModel.isRegularExpressions = false
  }

  fun plotJumpLocations(): List<JumpInfo> {
    return tagMap.values.map {
      JumpInfo(tagMap.inverse()[it]!!, queryString, it, editor)
    }
  }

  fun reset() {
    tagMap = HashBiMap.create()
    unseen1grams = linkedSetOf()
    jumpLocations = emptyList()
  }
}