package com.johnlindquist.acejump;

import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/24/12
 * Time: 2:37 PM
 */
public class AceJumper {
    private EditorImpl editor;
    private DocumentImpl document;


    public AceJumper(EditorImpl editor, DocumentImpl document) {
        this.editor = editor;
        this.document = document;
    }

    public void moveCaret(Integer offset) {
        editor.getCaretModel().moveToOffset(offset);
    }

    public void selectWordAtCaret() {
        CharSequence text = document.getCharsSequence();
        List<TextRange> ranges = new ArrayList<TextRange>();
        SelectWordUtil.addWordSelection(false, text, editor.getCaretModel().getOffset(), ranges);
        if (ranges.isEmpty()) return;

        int startWordOffset = Math.max(0, ranges.get(0).getStartOffset());
        int endWordOffset = Math.min(ranges.get(0).getEndOffset(), document.getTextLength());

        if (ranges.size() == 2 && editor.getSelectionModel().getSelectionStart() == startWordOffset &&
                editor.getSelectionModel().getSelectionEnd() == endWordOffset) {
            startWordOffset = Math.max(0, ranges.get(1).getStartOffset());
            endWordOffset = Math.min(ranges.get(1).getEndOffset(), document.getTextLength());
        }

        editor.getSelectionModel().setSelection(startWordOffset, endWordOffset);
    }

    public void setSelectionFromCaretToOffset(Integer offset) {
        editor.getSelectionModel().removeSelection();
        int caretOffset = editor.getCaretModel().getOffset();
        int offsetModifier =
                offset > caretOffset
                ? 1 : 0;
        editor.getSelectionModel().setSelection(caretOffset, offset + offsetModifier);
    }
}
