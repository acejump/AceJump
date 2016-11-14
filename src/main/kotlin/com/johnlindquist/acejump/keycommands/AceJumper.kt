package com.johnlindquist.acejump.keycommands

import com.intellij.codeInsight.editorActions.SelectWordUtil.addWordSelection
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.TextRange
import java.util.*

open class AceJumper(var editor: EditorImpl, var document: CharSequence) {
  fun moveCaret(offset: Int) {
    editor.selectionModel.removeSelection()
    editor.caretModel.moveToOffset(offset)
  }

  fun selectWordAtCaret() {
    val ranges = ArrayList<TextRange>()
    addWordSelection(false, document, editor.caretModel.offset, ranges)

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
