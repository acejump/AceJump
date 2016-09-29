package com.johnlindquist.acejump.search

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.util.EventDispatcher
import java.util.regex.Pattern
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class AceFinder(val findManager: FindManager, val editor: EditorImpl) {
  val document = editor.document as DocumentImpl
  val eventDispatcher = EventDispatcher.create(ChangeListener::class.java)
  val findModel: FindModel = createFindModel(findManager)
  var jumpLocations: Collection<Int> = emptyList()
  var tagMap: BiMap<String, Int> = HashBiMap.create()
  var qwertyAdjacentKeys =
    mapOf('1' to "12q", '2' to "23wq1", '3' to "34ew2", '4' to "45re3",
      '5' to "56tr4", '6' to "67yt5", '7' to "78uy6", '8' to "89iu7",
      '9' to "90oi8", '0' to "0po9", 'q' to "q12wa", 'w' to "w3esaq2",
      'e' to "e4rdsw3", 'r' to "r5tfde4", 't' to "t6ygfr5", 'y' to "y7uhgt6",
      'u' to "u8ijhy7", 'i' to "i9okju8", 'o' to "o0plki9", 'p' to "plo0",
      'a' to "aqwsz", 's' to "sedxzaw", 'd' to "drfcxse", 'f' to "ftgvcdr",
      'g' to "gyhbvft", 'h' to "hujnbgy", 'j' to "jikmnhu", 'k' to "kolmji",
      'l' to "lkop", 'z' to "zasx", 'x' to "xzsdc", 'c' to "cxdfv",
      'v' to "vcfgb", 'b' to "bvghn", 'n' to "nbhjm", 'm' to "mnjk")

  private fun determineJumpLocations(): Collection<Int> {
    val (startIndex, endIndex) = getVisibleRange()
    val fullText = document.charsSequence
    val window = fullText.substring(startIndex, endIndex)
    val sitesToCheck = getSitesToCheck(window).map { it + startIndex }
    val existingDigraphs = findDigraphs(fullText, sitesToCheck)
    tagMap = mapUniqueDigraphs(existingDigraphs)
    return tagMap.values
  }

  fun getSitesToCheck(window: String): Iterable<Int> {
    if (findModel.stringToFind.isEmpty())
      return 0..(window.length - 2)

    val indicesToCheck = arrayListOf<Int>()
    var result = findManager.findString(window, 0, findModel)
    while (result.isStringFound) {
      indicesToCheck.add(result.endOffset)
      result = findManager.findString(window, 0, findModel)
    }
    return indicesToCheck
  }

  fun findDigraphs(text: CharSequence, sites: Iterable<Int>):
    Multimap<String, Int> {
    val stringToIndex: Multimap<String, Int> = LinkedListMultimap.create()
    for (site in sites) {
      val (c1, c2) = Pair(text[site], text[site + 1])
      stringToIndex.put("$c1", site)
      if (c1.isLetterOrDigit() && c2.isLetterOrDigit()) {
        stringToIndex.put("$c1$c2", site)
      }
    }

    return stringToIndex
  }

  fun mapUniqueDigraphs(digraphs: Multimap<String, Int>): BiMap<String, Int> {
    val tagMap: BiMap<String, Int> = HashBiMap.create()
    for ((key, value) in digraphs.asMap()) {
      if (value.size == 1 && !tagMap.containsValue(value.first())) {
        var tag = key.toLowerCase()
        if (findModel.stringToFind.isEmpty())
          tag = tag.toLowerCase().replace(Regex("."), " ")
        else
          tag = findModel.stringToFind.replace(Regex("."), " ") + tag

        tagMap[tag] = value.first() - findModel.stringToFind.length
      }
    }

    for (c1 in 'a'..'z') {
      if (!digraphs.containsKey("$c1")) {
        val inverse = tagMap.inverse()
        for (index in inverse.keys) {
          if (!hasNearbyTag(index, inverse)) {
            tagMap.put("$c1", index)
          }
        }
      }
    }

    return tagMap
  }

  private fun hasNearbyTag(index: Int, assigned: BiMap<Int, String>): Boolean {
    val SPACING = 5

    for (i in (index - SPACING)..(index + SPACING)) {
      if (assigned.containsKey(i))
        return true
    }

    return false
  }

  fun findText(text: String) {
    println("Search box contents: " + text)
    findModel.stringToFind = text

    val application = ApplicationManager.getApplication()
    application.runReadAction({
      jumpLocations = determineJumpLocations()
    })
    application.invokeLater({
      if (text.isNotEmpty())
        eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
    })
  }

  private fun getVisibleRange(): Pair<Int, Int> {
    val firstVisibleLine = getVisualLineAtTopOfScreen(editor)
    val firstLine = visualLineToLogicalLine(editor, firstVisibleLine)
    val startOffset = getLineStartOffset(editor, firstLine)

    val height = getScreenHeight(editor)
    val lastLine = visualLineToLogicalLine(editor, firstVisibleLine + height)
    var endOffset = getLineEndOffset(editor, lastLine, true)
    endOffset = normalizeOffset(editor, lastLine, endOffset, true)

    return Pair(startOffset, endOffset)
  }

  fun createFindModel(findManager: FindManager): FindModel {
    val clone = findManager.findInFileModel.clone()
    clone.isFindAll = true
    clone.isFromCursor = true
    clone.isForward = true
    clone.isRegularExpressions = false
    clone.isWholeWordsOnly = false
    clone.isCaseSensitive = false
    clone.setSearchHighlighters(true)
    clone.isPreserveCase = false

    return clone
  }

  fun addResultsReadyListener(changeListener: ChangeListener) {
    eventDispatcher.addListener(changeListener)
  }

  fun findText(text: REGEX) {
    findModel.isRegularExpressions = true
    findText(text.pattern)
    findModel.isRegularExpressions = false
  }
}