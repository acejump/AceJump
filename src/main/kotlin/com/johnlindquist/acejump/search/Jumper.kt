package com.johnlindquist.acejump.search

import com.intellij.codeInsight.editorActions.SelectWordUtil.addWordSelection
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.openapi.fileEditor.impl.IdeDocumentHistoryImpl
import com.intellij.openapi.util.TextRange
import com.johnlindquist.acejump.search.Finder.originalQuery
import com.johnlindquist.acejump.ui.AceUI.document
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.project
import com.johnlindquist.acejump.ui.JumpInfo
import java.lang.Math.max
import java.lang.Math.min
import java.util.*

object Jumper {
  @Volatile
  var hasJumped = false

  fun jump(jumpInfo: JumpInfo) {
    if (originalQuery.last().isUpperCase())
      setSelectionFromCaretToOffset(jumpInfo.index)
    else if (Finder.targetModeEnabled) {
      // Moving the caret will trigger a reset, flipping targetModeEnabled, so
      // we need to move the caret and select the word in one single transaction
      moveCaret(jumpInfo.index)
      selectWordAtCaret()
    } else {
      moveCaret(jumpInfo.index)
    }

    hasJumped = true
  }

  fun moveCaret(offset: Int) =
    with(editor) {
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

  fun selectWordAtCaret() {
    val ranges = ArrayList<TextRange>()
    addWordSelection(false, document, editor.caretModel.offset, ranges)

    if (ranges.isEmpty()) return

    val firstRange = ranges[0]
    val startOfWordOffset = max(0, firstRange.startOffset)
    val endOfWordOffset = min(firstRange.endOffset, document.length)

    with(editor.selectionModel) {
      removeSelection()
      setSelection(startOfWordOffset, endOfWordOffset)
    }
  }

  fun setSelectionFromCaretToOffset(toOffset: Int) =
    with(editor) {
      selectionModel.removeSelection()
      selectionModel.setSelection(caretModel.offset, toOffset)
      caretModel.moveToOffset(toOffset)
    }
}
