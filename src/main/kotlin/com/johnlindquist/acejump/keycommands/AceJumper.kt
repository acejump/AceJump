package com.johnlindquist.acejump.keycommands

import com.intellij.codeInsight.editorActions.SelectWordUtil
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.TextRange
import java.util.*

open class AceJumper(var editor: EditorImpl, var document: DocumentImpl) {
  fun moveCaret(offset: Int) {
    editor.caretModel.moveToOffset(offset)
  }

  fun selectWordAtCaret() {
    val text = document.charsSequence
    val ranges = ArrayList<TextRange>()
    SelectWordUtil.addWordSelection(false, text, editor.caretModel.offset, ranges)

    if (ranges.isEmpty())
      return

    val startWordOffset = Math.max(0, ranges[0].startOffset)
    val endWordOffset = Math.min(ranges[0].endOffset, document.textLength)

    editor.selectionModel.setSelection(startWordOffset, endWordOffset)
  }

  fun setSelectionFromCaretToOffset(toOffset: Int) {
    editor.selectionModel.removeSelection()
    editor.selectionModel.setSelection(editor.caretModel.offset, toOffset)
  }
}