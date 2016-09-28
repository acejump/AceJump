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
  var uniqueJumpLocations: BiMap<String, Int> = HashBiMap.create()
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

  val textAndOffsetHash = HashMap<String, Int>()

  private fun findUniqueJumpLocations(target: String): BiMap<String, Int> {
    var (startIndex, endIndex) = getVisibleRange()
    val uniqueJumpLocations = HashBiMap.create<String, Int>()
    val stringToIndex = LinkedListMultimap.create<String, Int>()
    val text = document.charsSequence.toString().substring(startIndex, endIndex)

    if (!target.isEmpty()) {
      val pattern = Pattern.compile("(?i)$target")
      val matcher = pattern.matcher(text)
      while (matcher.find()) {
        if (matcher.end() < endIndex && text[matcher.end()].isLetterOrDigit()) {
          stringToIndex.put(text[matcher.end()].toString(), matcher.start())
          if (matcher.end() + 1 < endIndex) {
            val nextTwoChars = text.substring(matcher.end(), matcher.end() + 2)
            if (nextTwoChars.matches(Regex("[a-z0-9]{2}")))
              stringToIndex.put(nextTwoChars, matcher.end())
          }
        }
      }
    } else {
      // Unique digraphs tend to bunch-up. Let's try to spread them out.
      var lastGoodIndex = 0
      var previousChar = text.first()
      for (char in text.substring(1)) {
        if (char.isLetterOrDigit() &&
          previousChar.isLetterOrDigit() &&
          ((startIndex - lastGoodIndex) > 10)) {
          lastGoodIndex = startIndex
          stringToIndex.put("$previousChar$char".toLowerCase(), startIndex)
        }
        stringToIndex.put("$char".toLowerCase(), startIndex)
        startIndex++
        previousChar = char
      }
    }

    assignRemainingDigraphs(stringToIndex, uniqueJumpLocations, text)
    return uniqueJumpLocations
  }

  private fun assignRemainingDigraphs(stringToIndex: Multimap<String, Int>,
                                      jumpLocations: BiMap<String, Int>,
                                      text: String) {
    for (key in stringToIndex.keys()) {
      val value = stringToIndex[key]
      if (value.size == 1)
        jumpLocations[key] = value.first()
    }

    for (c1 in 'a'..'z') {
      for (c2 in 'a'..'z') {
        if (!stringToIndex.containsKey("$c2$c1")) {
          val pattern = Pattern.compile("(?i)$c2[\\w]+$c1")
          val matcher = pattern.matcher(text)
          if (matcher.find() && !matcher.find()) {
            matcher.find(0)
            stringToIndex.put("$c2$c1", matcher.start())
          }
        }
      }
    }
  }

  fun findText(text: String, isRegEx: Boolean) {
    findModel.stringToFind = text
    findModel.isRegularExpressions = isRegEx

    val application = ApplicationManager.getApplication()
    application.runReadAction({
      uniqueJumpLocations = findUniqueJumpLocations(text)
      if (text.isNotEmpty())
        jumpLocations = findJumpLocations()
      else jumpLocations = uniqueJumpLocations.values
    })
    application.invokeLater({
      uniqueJumpLocations = findUniqueJumpLocations(text)
      if (text.isNotEmpty())
        eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
    })
  }

  private fun findJumpLocations(): List<Int> {
    val (startIndex, endIndex) = getVisibleRange()
    val text = document.charsSequence.toString().substring(startIndex, endIndex)

    val offsets = ArrayList<Int>()
    var match = findManager.findString(text, 0, findModel, virtualFile)
    while (match.isStringFound) {
      offsets.add(startIndex + match.startOffset)
      match = findManager.findString(text, match.endOffset, findModel, virtualFile)
    }

    return offsets
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
      val str = uniqueJumpLocations.inverse()[textOffset]!!

      if (text.isEmpty() || str.startsWith(text)) {
        val point: RelativePoint = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset))
        textPointPairs.add(Pair(str, point.originalPoint as Point))
        textAndOffsetHash[str] = textOffset
      }
    }

    return textPointPairs
  }
}