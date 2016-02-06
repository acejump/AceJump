package com.johnlindquist.acejump

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

        var startWordOffset = Math.max(0, ranges[0].startOffset)
        var endWordOffset = Math.min(ranges[0].endOffset, document.textLength)

/*        if(ranges.size() == 2 && editor.getSelectionModel().getSelectionStart() == startWordOffset && editor.getSelectionModel().getSelectionEnd() == endWordOffset)
            startWordOffset = Math.max(0, ranges.get(1).getStartOffset())
        endWordOffset = Math.min(ranges.get(1).getEndOffset(), document.getTextLength())*/

        editor.selectionModel.setSelection(startWordOffset, endWordOffset);
    }

    fun setSelectionFromCaretToOffset(offset: Int) {
        editor.selectionModel.removeSelection()
        val caretOffset = editor.caretModel.offset
        editor.selectionModel.setSelection(caretOffset, offset);
    }

}