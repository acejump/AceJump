package com.johnlindquist.acejump.search

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
  var results: List<Int> = emptyList()
//  var results: Map<Int, String> = mapOf()
  var stringToIndex: Multimap<String, Int> = LinkedListMultimap.create()
  var getEndOffset = false
  var firstChar = ""
  var customOffset = 0
  var isTargetMode = false
  val resultComparator = ResultComparator(document, editor)
  val textAndOffsetHash = HashMap<String, Int>()

  fun findText(text: String, isRegEx: Boolean) {
    findModel.stringToFind = text
    findModel.isRegularExpressions = isRegEx

    val application = ApplicationManager.getApplication()
    application.runReadAction({ results = findAllVisible() })
    application.invokeLater({
      results = results.sortedWith(resultComparator)

      startResult = 0
      endResult = allowedCount

      eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
    })
  }

  fun findAllVisible(): List<Int> {
    //System.out.println("----- findAllVisible")
    val visualLineAtTopOfScreen = getVisualLineAtTopOfScreen(editor)
    val firstLine = visualLineToLogicalLine(editor, visualLineAtTopOfScreen)
    val startOffset = getLineStartOffset(editor, firstLine)

    val height = getScreenHeight(editor)
    val lastLine = visualLineToLogicalLine(editor, visualLineAtTopOfScreen + height)
    val endOffset = normalizeOffset(editor, lastLine, getLineEndOffset(editor, lastLine, true), true)

    val text = document.charsSequence.toString().substring(startOffset, endOffset)
    val offsets = ArrayList<Int>()

    var offset = 0
    var result = findManager.findString(text, offset, findModel, virtualFile)
    while (result.isStringFound) {
      val resultOffset =
              if (getEndOffset)
                result.endOffset - 1
              else
                result.startOffset

      offsets.add(startOffset + resultOffset + customOffset)
      offset = result.endOffset
      result = findManager.findString(text, offset, findModel, virtualFile)
    }

    return offsets
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

  fun setupJumpLocations(): MutableList<Pair<String, Point>> {
    val textPointPairs: MutableList<Pair<String, Point>> = ArrayList()

    if (results.size == 0)
      return textPointPairs //todo: hack, in case random keystrokes make it through

    textAndOffsetHash.clear()
    val total = results.size - 1

    val letters = "abcdefghijklmnopqrstuvwxyz"
    val len = letters.length
    val groups = Math.floor(total.toDouble() / len)
    //print("groups: " + groups.toString())
    val lenMinusGroups = len - groups.toInt()
    //print("last letter: " + letters.charAt(lenMinusGroups).toString() + "\n")

    var i = 0
    for (textOffset in results) {
      var str = ""

      val iGroup = i - lenMinusGroups
      val iModGroup = iGroup % len
      //if(iModGroup == 0) print("================\n")
      val i1 = Math.floor(lenMinusGroups.toDouble() + ((i + groups.toInt()) / len)).toInt() - 1
      if (i >= lenMinusGroups) {
        str += letters.elementAt(i1)
        str += letters.elementAt(iModGroup).toString()
      } else {
        str += letters.elementAt(i).toString()
      }
      //print(i.toString() + ": " + str + "     iModGroup:" + iModGroup.toString() + "\n")

      val point: RelativePoint = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset))
      textPointPairs.add(Pair(str, point.originalPoint as Point))
      textAndOffsetHash[str] = textOffset

      if (str == "zz") {
        break
      }
      i++
    }

    return textPointPairs
  }
}