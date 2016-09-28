package com.johnlindquist.acejump.search

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.LinkedListMultimap
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
    val ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyz"
    val END_OF_LINE = "\\n"
    val BEGINNING_OF_LINE = "^.|\\n(?<!.\\n)"
    val CODE_INDENTS = "^\\s*\\S"
    val WHITE_SPACE = "\\s+\\S(?<!^\\s*\\S)"
  }

  val document = editor.document as DocumentImpl
  val eventDispatcher: EventDispatcher<ChangeListener> = EventDispatcher.create(ChangeListener::class.java)
  val findModel: FindModel = createFindModel(findManager)
  var startResult = 0
  var endResult = 0
  var allowedCount = ALLOWED_CHARACTERS.length
  var jumpLocations: Collection<Int> = emptyList()
  var uniqueJumpLocations: BiMap<String, Int> = HashBiMap.create()
  //  var results: Map<Int, String> = mapOf()
  var firstChar = ""
  var isTargetMode = false

  val resultComparator = ResultComparator(document, editor)
  val textAndOffsetHash = HashMap<String, Int>()

  fun findUniqueJumpLocations(): BiMap<String, Int> {
    var (startIndex, endIndex) = getVisibleRange()
    val uniqueJumpLocations = HashBiMap.create<String, Int>()
    val stringToIndex = LinkedListMultimap.create<String, Int>()
    val text = document.charsSequence.toString().substring(startIndex, endIndex)

    // Unique digraphs tend to bunch-up. Let's try to spread them out.
    var lastGoodIdx = 0
    var previousChar = text.first()
    for (char in text.substring(1)) {
      if (char.isLetter() && previousChar.isLetter() && ((startIndex -
              lastGoodIdx) > 10)) {
        lastGoodIdx = startIndex
        stringToIndex.put("$previousChar$char".toLowerCase(), startIndex)
      }
      startIndex++
      previousChar = char
    }

    for (key in stringToIndex.keys()) {
      val value = stringToIndex[key]
      if (value.size == 1)
        uniqueJumpLocations[key] = value.first()
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

    return uniqueJumpLocations
  }

  fun findText(text: String, isRegEx: Boolean) {
    findModel.stringToFind = text
    findModel.isRegularExpressions = isRegEx

    val application = ApplicationManager.getApplication()
    application.runReadAction({
      uniqueJumpLocations = findUniqueJumpLocations()
      if (text.isNotEmpty())
        jumpLocations = findJumpLocations()
      else jumpLocations = uniqueJumpLocations.values
    })
    application.invokeLater({
      jumpLocations = jumpLocations.sortedWith(resultComparator)

      startResult = 0
      endResult = allowedCount

      eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
    })
  }

  fun findJumpLocations(): List<Int> {
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

  fun getVisibleRange(): Pair<Int, Int> {
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

  fun markJumpLocations(): MutableList<Pair<String, Point>> {
    val textPointPairs: MutableList<Pair<String, Point>> = ArrayList()

    if (jumpLocations.size == 0)
      return textPointPairs //todo: hack, in case random keystrokes make it through

    textAndOffsetHash.clear()
    val total = jumpLocations.size - 1

    val letters = "abcdefghijklmnopqrstuvwxyz"
    val len = letters.length
    val groups = Math.floor(total.toDouble() / len)
    //print("groups: " + groups.toString())
    val lenMinusGroups = len - groups.toInt()
    //print("last letter: " + letters.charAt(lenMinusGroups).toString() + "\n")

    var i = 0
    for (textOffset in jumpLocations) {
      var str = uniqueJumpLocations.inverse()[textOffset]!!

//      val iGroup = i - lenMinusGroups
//      val iModGroup = iGroup % len
//      //if(iModGroup == 0) print("================\n")
//      val i1 = Math.floor(lenMinusGroups.toDouble() + ((i + groups.toInt()) / len)).toInt() - 1
//      if (i >= lenMinusGroups) {
//        str += letters.elementAt(i1)
//        str += letters.elementAt(iModGroup).toString()
//      } else {
//        str += letters.elementAt(i).toString()
//      }
//      //print(i.toString() + ": " + str + "     iModGroup:" + iModGroup.toString() + "\n")

      if (firstChar.isEmpty() || str.startsWith(firstChar)) {
        val point: RelativePoint = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset))
        textPointPairs.add(Pair(str, point.originalPoint as Point))
        textAndOffsetHash[str] = textOffset
      }

//      if (str == "zz") {
//        break
//      }
      i++
    }

    return textPointPairs
  }
}