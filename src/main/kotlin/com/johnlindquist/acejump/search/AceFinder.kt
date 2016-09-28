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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.EventDispatcher
import java.awt.Point
import java.util.*
import java.util.regex.Pattern
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class AceFinder(val findManager: FindManager, val editor: EditorImpl, val virtualFile: VirtualFile) {
  companion object {
    val END_OF_LINE = "\\n"
    val BEGINNING_OF_LINE = "^.|\\n(?<!.\\n)"
    val CODE_INDENTS = "^\\s*\\S"
    val WHITE_SPACE = "\\s+\\S(?<!^\\s*\\S)"
  }

  val document = editor.document as DocumentImpl
  val eventDispatcher: EventDispatcher<ChangeListener> = EventDispatcher.create(ChangeListener::class.java)
  val findModel: FindModel = createFindModel(findManager)
  var jumpLocations: Collection<Int> = emptyList()
  var textAndOffsetHash: BiMap<String, Int> = HashBiMap.create()
  var isTargetMode = false
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

  private fun findJumpLocations(target: String): Collection<Int> {
    val (startIndex, endIndex) = getVisibleRange()
    val fullText = document.charsSequence
    val window = fullText.substring(startIndex, endIndex)

    val sitesToCheck =
      if (target.isEmpty())
        startIndex..(endIndex - 1)
      else {
        val indicesToCheck = arrayListOf<Int>()
        val match = Pattern.compile("(?i)$target").matcher(window)
        while (match.find()) {
          indicesToCheck.add(match.end())
        }
        indicesToCheck + startIndex
      }

    val existingDigraphs = findDigraphs(fullText, sitesToCheck)
    val completeDigraphs = assignRemainingDigraphs(existingDigraphs)
    textAndOffsetHash = completeDigraphs

    return completeDigraphs.values
  }

  private fun findDigraphs(text: CharSequence, sites: Iterable<Int>):
    Multimap<String, Int> {
    val stringToIndex: Multimap<String, Int> = LinkedListMultimap.create()
    for (site in sites) {
      val (c1, c2) = Pair(text[site], text[site + 1])
      if (c1.isLetterOrDigit()) {
        stringToIndex.put("$c1", site)
        if (c2.isLetterOrDigit()) {
          stringToIndex.put("$c1$c2", site)
        }
      }
    }

    return stringToIndex
  }

  private fun assignRemainingDigraphs(currentDigraphs: Multimap<String, Int>): BiMap<String, Int> {
    val jumpLocations: BiMap<String, Int> = HashBiMap.create()
    for (key in currentDigraphs.keys()) {
      val value = currentDigraphs[key]
      if (value.size == 1)
        jumpLocations[key] = value.first()
    }



    return jumpLocations
  }

  fun findText(text: String, isRegEx: Boolean) {
    findModel.stringToFind = text
    findModel.isRegularExpressions = isRegEx

    val application = ApplicationManager.getApplication()
    application.runReadAction({
      jumpLocations = findJumpLocations(text)
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

  fun markJumpLocations(text: String): MutableList<Pair<String, Point>> {
    val textPointPairs = ArrayList<Pair<String, Point>>()

    if (jumpLocations.size == 0)
      return textPointPairs //todo: hack, in case random keystrokes make it through

    textAndOffsetHash.clear()

    for (textOffset in jumpLocations) {
      val str = textAndOffsetHash.inverse()[textOffset]!!

      if (text.isEmpty() || str.startsWith(text)) {
        val point: RelativePoint = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset))
        textPointPairs.add(Pair(str, point.originalPoint as Point))
        textAndOffsetHash[str] = textOffset
      }
    }

    return textPointPairs
  }
}