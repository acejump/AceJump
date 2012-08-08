package com.johnlindquist.acejump;

import java.awt.event.KeyEvent;

/**
 * User: John Lindquist
 * Date: 9/8/11
 * Time: 12:10 AM
 */
public class AceJumpSelectFromCaretAction extends AceJumpAction {


    private int offsetModifier;

    @Override
    protected void moveCaret(Integer offset) {

    }

    @Override
    protected void applyModifier(KeyEvent e) {
        offsetModifier = 0;
        if (e.isAltDown()) {
            offsetModifier += 1;
        }
    }

    @Override
    protected void completeCaretMove(Integer offset) {
        editor.getSelectionModel().removeSelection();
        int caretOffset = caretModel.getOffset();
        if (offset < caretOffset) {
            offset = offset + searchBox.getText().length();
        }
        editor.getSelectionModel().setSelection(caretOffset, offset + offsetModifier);
    }
}
