package com.johnlindquist.acejump.search

import com.intellij.codeInsight.editorActions.SelectWordUtil.addWordSelection
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.util.TextRange
import com.johnlindquist.acejump.search.Finder.origQ
import com.johnlindquist.acejump.view.Marker
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import com.johnlindquist.acejump.view.Model.project
import java.lang.Math.max
import java.lang.Math.min
import java.util.*

object Jumper {
  @Volatile
  var hasJumped = false

  fun jump(marker: Marker) = editor.run {
    if (origQ.last().isUpperCase())
      selectFromToOffset(caretModel.offset, marker.index)
    else if (Finder.targetModeEnabled) {
      // Moving the caret will trigger a reset, flipping targetModeEnabled, so
      // we need to move the caret and select the word in one single transaction
      moveCaret(marker.index)
      selectWordAtOffset(marker.index)
    } else moveCaret(marker.index)

    hasJumped = true
  }

  fun Editor.moveCaret(offset: Int) {
    // Add current caret position to navigation history
    CommandProcessor.getInstance().executeCommand(project,
      aceJumpHistoryAppender, "AceJumpHistoryAppender",
      DocCommandGroupId.noneGroupId(document), document)

    selectionModel.removeSelection()
    caretModel.moveToOffset(offset)
  }

  private val aceJumpHistoryAppender = {
    with(IdeDocumentHistory.getInstance(project) as IdeDocumentHistoryImpl) {
      onSelectionChanged()
      includeCurrentCommandAsNavigation()
      includeCurrentPlaceAsChangePlace()
    }
  }

  fun Editor.selectWordAtOffset(offset: Int = caretModel.offset) {
    val ranges = ArrayList<TextRange>()
    addWordSelection(false, editorText, offset, ranges)

    if (ranges.isEmpty()) return

    val firstRange = ranges[0]
    val startOfWordOffset = max(0, firstRange.startOffset)
    val endOfWordOffset = min(firstRange.endOffset, editorText.length)

    selectFromToOffset(startOfWordOffset, endOfWordOffset)
  }
}