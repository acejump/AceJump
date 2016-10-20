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

class AceFinder(val findManager: FindManager, val editor: EditorImpl) {
  val document = editor.document as DocumentImpl
  val eventDispatcher = EventDispatcher.create(ChangeListener::class.java)
  val findModel: FindModel = findManager.findInFileModel.clone()
  var jumpLocations: Collection<JumpInfo> = emptyList()
  var tagMap: BiMap<String, Int> = HashBiMap.create()
  val maxTags = 26
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

  fun findText(text: String, key: Char) {
    if (key == 0.toChar())
      reset()

    findModel.stringToFind = text
    populateNgrams()

    val application = ApplicationManager.getApplication()
    application.runReadAction(jump(key))
    application.invokeLater({
      if (text.isNotEmpty())
        eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
    })
  }

  private fun populateNgrams() {
    unseen1grams.addAll(('a'..'z').mapTo(linkedSetOf(), { "$it" }))
    unseen1grams.addAll(('0'..'9').mapTo(linkedSetOf(), { "$it" }))
    adjacent.flatMapTo(unseen2grams, { e -> e.value.map { c -> "$c${e.key}" } })
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
      //todo: refactor this mess
      val text = findModel.stringToFind.toLowerCase()
      var jumped = false
      if (text.isNotEmpty() && getSitesInView(document.charsSequence.toString().toLowerCase()).size <= 1) {
        if (tagMap.containsKey(text)) {
          jumpTo(JumpInfo(text, text, tagMap[text]!!, editor))
          jumped = true
        } else if (2 <= text.length) {
          val last1: String = text.substring(text.length - 1)
          val last2: String = text.substring(text.length - 2)
          if (tagMap.containsKey(last2)) {
            jumpTo(JumpInfo(last2, text, tagMap[last2]!!, editor))
            jumped = true
          } else if (tagMap.containsKey(last1)) {
            jumpTo(JumpInfo(last1, text, tagMap[last1]!!, editor))
            jumped = true
          }
        }
      }

      if (!jumped) {
        jumpLocations = determineJumpLocations()
        if (jumpLocations.size == 1)
          jumpTo(jumpLocations.first())
      }
    }
  }

  var sitesToCheck: List<Int> = listOf()

  private fun determineJumpLocations(): Collection<JumpInfo> {
    val fullText = document.charsSequence.toString().toLowerCase()
    sitesToCheck = getSitesInView(fullText)
    val existingDigraphs = makeMap(fullText, sitesToCheck)
    if (existingDigraphs.isEmpty)
      tagMap = filterTags(tagMap, findModel.stringToFind)
    else
      tagMap = compact(mapUniqueDigraphs(existingDigraphs))
    return plotJumpLocations()
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
        !findModel.stringToFind.endsWith(firstCharacter) &&
        !findModel.stringToFind.endsWith(e.key))
        firstCharacter
      else e.key
    })

  private fun getSitesInView(fullText: String): List<Int> {
    fun getSitesToCheck(window: String): Iterable<Int> {
      if (findModel.stringToFind.isEmpty())
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
      val (p1, p2) = Pair(site, site + 1)
      val (c1, c2) = Pair(text[p1], text[p2])
      val origin = p1 - findModel.stringToFind.length
      stringToIndex.put("$c1", origin)
      stringToIndex.put("$c1$c2", origin)
      unseen1grams.remove("$c1")
      unseen2grams.remove("$c1$c2")
      unseen2grams.removeAll { it.startsWith(c1) }
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
      val search = findModel.stringToFind
      if (search == entry.key) {
        newTagMap[entry.key] = entry.value
      } else if (search.last() == entry.key.first()) {
        newTagMap[entry.key] = entry.value
        unusedNgrams.remove(entry.key[0].toString())
      }
    }

    //Assign unique tags first
    val (g1, gt) = digraphs.asMap().entries.partition {
      it.value.size == 1 && it.key.all(Char::isLetterOrDigit)
    }
    g1.forEach { tryToAssignTagToIndex(it.value.first(), it.key) }

    val remaining = gt.sortedByDescending { it.value.size }
    var tagsNeeded = remaining.sumBy { it.value.size } - unseen1grams.size
    val bigramIterator = unseen2grams.sortedBy(String::last).iterator()
    while (bigramIterator.hasNext() && 0 <= tagsNeeded--) {
      val biGram = bigramIterator.next()
      unusedNgrams.remove(biGram[0].toString())
      unusedNgrams.add(biGram)
    }

    val remainingSites = remaining.filter {
      it.key.first().isLetterOrDigit() || findModel.stringToFind.isNotEmpty()
    }.flatMap { it.value }.listIterator()

    while (remainingSites.hasNext() && unusedNgrams.isNotEmpty())
      tryToAssignTagToIndex(remainingSites.next(), unusedNgrams.first())

    return newTagMap
  }

  fun findText(text: Regexp) {
    findModel.isRegularExpressions = true
    findText(text.pattern, 0.toChar())
    findModel.isRegularExpressions = false
  }

  fun plotJumpLocations(): List<JumpInfo> {
    return tagMap.values.map {
      JumpInfo(tagMap.inverse()[it]!!, findModel.stringToFind, it, editor)
    }
  }

  fun reset() {
    tagMap = HashBiMap.create()
    unseen1grams = linkedSetOf()
    jumpLocations = emptyList()
  }
}