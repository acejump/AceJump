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
  var unseenUnigrams: LinkedHashSet<String> = linkedSetOf()
  var unseenBigrams: LinkedHashSet<String> = linkedSetOf()
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
    findModel.setSearchHighlighters(true)
    findModel.isPreserveCase = false
  }

  fun findText(text: String, key: Char) {
    if (key == 0.toChar())
      reset()

    findModel.stringToFind = text
    unseenUnigrams = ('a'..'z').mapTo(linkedSetOf(), { "$it" })
    adjacent.flatMapTo(unseenBigrams, { e ->
      e.value.map { c ->
        "${e.key}$c"
      }
    })
//    unseenBigrams.addAll(('a'..'z').flatMapTo(linkedSetOf(), { a ->
//      ('a'..'z').map { b -> "$a$b" }
//    }))

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
      //todo: refactor this mess
      val text = findModel.stringToFind.toLowerCase()
      var jumped = false
      if (text.isNotEmpty() && getSitesInView().isEmpty()) {
        if (tagMap.containsKey(text)) {
          jumpTo(JumpInfo(text, text, tagMap[text]!!, editor))
          jumped = true
        } else if (1 < text.length) {
          val last1: String = text.substring(text.length - 1)
          if (tagMap.containsKey(last1)) {
            jumpTo(JumpInfo(last1, text, tagMap[last1]!!, editor))
            jumped = true
          } else if (2 < text.length) {
            val last2: String = text.substring(text.length - 2)
            if (tagMap.containsKey(last2)) {
              jumpTo(JumpInfo(last2, text, tagMap[last2]!!, editor))
              jumped = true
            }
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
    sitesToCheck = getSitesInView()
    val existingDigraphs = makeMap(fullText, sitesToCheck)
    tagMap = mapUniqueDigraphs(existingDigraphs)
    return plotJumpLocations()
  }

  private fun getSitesInView(): List<Int> {
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

    val fullText = document.charsSequence.toString().toLowerCase()
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
      unseenUnigrams.removeAll(listOf("$c1", "$c2"))
      unseenBigrams.remove("$c1$c2")
      unseenBigrams.removeAll { it.startsWith(c1) }
    }

    return stringToIndex
  }

  fun mapUniqueDigraphs(digraphs: Multimap<String, Int>): BiMap<String, Int> {
    val newTagMap: BiMap<String, Int> = HashBiMap.create()
    val unusedNgrams = unseenUnigrams
    //todo: decide on a word-by-word basis
    fun hasNearbyTag(index: Int): Boolean {
      var gap = (digraphs.size().toDouble() / unusedNgrams.size / 2).toInt()
      gap = if (gap < 2) 2 else gap
      val spread = (index - gap)..(index + gap)
      return spread.any { newTagMap.containsValue(it) }
    }

    fun hasNearbyTag2(index: Int): Boolean {
      val gap = 5
      val spread = (index - gap)..(index + gap)
      return spread.any { digraphs.containsValue(it) }
    }

    fun tryToAssignTagToIndex(index: Int, tag: String) {
      if (!hasNearbyTag(index)) {
        var choosenTag = tag
        val oldTag = tagMap.inverse()[index]
        if (oldTag != null) {
          choosenTag = oldTag
          if (unusedNgrams.contains(choosenTag[0].toString()))
            choosenTag = choosenTag[0].toString()
        }

        if (!newTagMap.containsValue(index)) {
          newTagMap[choosenTag] = index
          if (choosenTag.length == 1) {
            unusedNgrams.removeAll(unusedNgrams.filter { it[0] == choosenTag[0] })
          } else {
            unusedNgrams.remove(choosenTag)
          }
        }
      }
    }

    if (digraphs.isEmpty) {
      return tagMap
    }

    tagMap.entries.forEach {
      val search = findModel.stringToFind
      if (search == it.key) {
        newTagMap.put(it.key, it.value)
      } else if (search.last() == it.key.first()) {
        newTagMap.put(it.key, it.value)
        unusedNgrams.remove(it.key[0].toString())
      }

      if (hasNearbyTag2(it.value)) {
        unusedNgrams.removeAll(it.key.map(Char::toString))
      }
    }

    newTagMap.keys.flatMap { it.toCharArray().toList() }.forEach {
      unusedNgrams.remove(it.toString())
    }

    //Unique tags first
    val (g1, gt) = digraphs.asMap().entries.partition { it.value.size == 1 }
    g1.filter { it.key.all(Char::isLetterOrDigit) }
      .forEach { tryToAssignTagToIndex(it.value.first(), it.key) }
    val (g2, gm) = gt.partition { it.key.all(Char::isLetterOrDigit) && it.value.size == 2 }
    g2.forEach {
      if (1 < unusedNgrams.size) {
        tryToAssignTagToIndex(it.value.first(), unusedNgrams.first())
        tryToAssignTagToIndex(it.value.last(), unusedNgrams.first())
      }
    }

    var tagsNeeded = gm.size - unseenUnigrams.size
    for (bigram in unseenBigrams) {
      if (0 <= tagsNeeded--) {
        unusedNgrams.remove(bigram[0].toString())
        unusedNgrams.add(bigram)
      } else {
        break
      }
    }

    newTagMap.keys.forEach { unusedNgrams.remove(it) }

    val g = gm.filter {
      it.key.first().isLetterOrDigit() ||
        findModel.stringToFind.isNotEmpty()
    }.flatMap { it.value }
    for (index in g) {
      if (unusedNgrams.isNotEmpty())
        tryToAssignTagToIndex(index, unusedNgrams.first())
      else
        break
    }

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
    unseenUnigrams = linkedSetOf()
    jumpLocations = emptyList()
  }
}