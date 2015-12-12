package com.johnlindquist.acejump

import com.intellij.codeInsight.editorActions.SelectWordUtil
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.util.TextRange
import java.util.*

public open class AceJumper(var editor: EditorImpl, var document: DocumentImpl) {

    fun moveCaret(offset: Int) {
        editor.getCaretModel().moveToOffset(offset)
    }

    fun selectWordAtCaret() {
        val text = document.getCharsSequence()
        val ranges = ArrayList<TextRange>()

        SelectWordUtil.addWordSelection(false, text, editor.getCaretModel().getOffset(), ranges)

        if (ranges.isEmpty())
            return

        var startWordOffset = Math.max(0, ranges.get(0).getStartOffset())
        var endWordOffset = Math.min(ranges.get(0).getEndOffset(), document.getTextLength())

/*        if(ranges.size() == 2 && editor.getSelectionModel().getSelectionStart() == startWordOffset && editor.getSelectionModel().getSelectionEnd() == endWordOffset)
            startWordOffset = Math.max(0, ranges.get(1).getStartOffset())
        endWordOffset = Math.min(ranges.get(1).getEndOffset(), document.getTextLength())*/

        editor.getSelectionModel().setSelection(startWordOffset, endWordOffset);
    }

    fun setSelectionFromCaretToOffset(offset: Int) {
        editor.getSelectionModel().removeSelection()
        val caretOffset = editor.getCaretModel().getOffset()
        editor.getSelectionModel().setSelection(caretOffset, offset);
    }

}