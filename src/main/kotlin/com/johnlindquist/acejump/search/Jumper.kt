package com.johnlindquist.acejump.search

import com.intellij.codeInsight.editorActions.SelectWordUtil.addWordSelection
import com.intellij.openapi.util.TextRange
import com.johnlindquist.acejump.ui.AceUI.document
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.JumpInfo
import java.util.*

object Jumper {
  var hasJumped = false
  fun jump(jumpInfo: JumpInfo) {
    if (jumpInfo.query.last().isUpperCase())
      setSelectionFromCaretToOffset(jumpInfo.index)
    else
      moveCaret(jumpInfo.index)

    if (Finder.targetModeEnabled)
      selectWordAtCaret()

    hasJumped = true
  }

  fun moveCaret(offset: Int) {
    editor.selectionModel.removeSelection()
    editor.caretModel.moveToOffset(offset)
  }

  fun selectWordAtCaret() {
    val ranges = ArrayList<TextRange>()
    addWordSelection(false, document, editor.caretModel
      .offset,
      ranges)

    if (ranges.isEmpty()) return

    val startWordOffset = Math.max(0, ranges[0].startOffset)
    val endWordOffset = Math.min(ranges[0].endOffset, document.length)

    editor.selectionModel.removeSelection()
    editor.selectionModel.setSelection(startWordOffset, endWordOffset)
  }

  fun setSelectionFromCaretToOffset(toOffset: Int) {
    editor.selectionModel.removeSelection()
    editor.selectionModel.setSelection(editor.caretModel.offset, toOffset)
    editor.caretModel.moveToOffset(toOffset)
  }
}
