package com.johnlindquist.acejump.ui;

import com.intellij.ui.popup.AbstractPopup;
import com.johnlindquist.acejump.keycommands.AceKeyCommand;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.HashMap;

/**
* Created with IntelliJ IDEA.
* User: johnlindquist
* Date: 8/24/12
* Time: 3:10 PM
* To change this template use File | Settings | File Templates.
*/
public class SearchBox extends JTextField {
    private final HashMap<Integer, AceKeyCommand> preProcessKeyReleasedMap = new HashMap<Integer, AceKeyCommand>();
    private final HashMap<Integer, AceKeyCommand> preProcessKeyPressedMap = new HashMap<Integer, AceKeyCommand>();

    protected boolean isPreProcessed;

    public boolean getIsSearchEnabled() {
        return isSearchEnabled && getText().length() == 1;
    }

    protected boolean isSearchEnabled = true;
    private AbstractPopup popupContainer;

    public void setDefaultKeyCommand(AceKeyCommand defaultKeyCommand) {
        this.defaultKeyCommand = defaultKeyCommand;
    }

    private AceKeyCommand defaultKeyCommand;

    @Override
    protected void paintBorder(Graphics g) {
        //do nothing
    }

    @Override
    protected void processKeyEvent(final KeyEvent keyEvent) {
        if (getText().length() == 0) {
            //todo: rethink the "isSearchEnabled" state approach. Works great now, could be cleaner
            isSearchEnabled = true;
        }

        AceKeyCommand aceKeyCommand = null;
        if (keyEvent.getID() == KeyEvent.KEY_RELEASED) {
            /*
                My lack of Java experience is probably showing here, but the only way I could find to handle "HOME" and
                "END" was to on "release". Is there a better convention for this?
             */
            aceKeyCommand = preProcessKeyReleasedMap.get(keyEvent.getKeyCode());
        }

        if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
            aceKeyCommand = preProcessKeyPressedMap.get(keyEvent.getKeyCode());
        }

        if (aceKeyCommand != null) {
            aceKeyCommand.execute(keyEvent);
            return;
        }

        super.processKeyEvent(keyEvent);
        if (keyEvent.getID() != KeyEvent.KEY_TYPED) return;

        if(defaultKeyCommand != null) defaultKeyCommand.execute(keyEvent);

        if (getText().length() == 2) {
            try {
                setText(getText(0, 1));
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }

    }

    public void setIsPreProcessed() {
        isPreProcessed = true;
    }

    public void forceSpaceChar() {
        setText(" ");
        disableSearchAndReturn();
    }

    public void disableSearchAndReturn() {
        disableSearch();
        setIsPreProcessed();
    }

    public AbstractPopup getPopupContainer() {
        return popupContainer;
    }

    public void setPopupContainer(AbstractPopup popupContainer) {
        this.popupContainer = popupContainer;
    }

    public void disableSearch() {
        isSearchEnabled = false;
    }

    public void addPreProcessReleaseKey(int key, AceKeyCommand keyCommand) {
        preProcessKeyReleasedMap.put(key, keyCommand);
    }

    public void addPreProcessPressedKey(int key, AceKeyCommand keyCommand) {
        preProcessKeyPressedMap.put(key, keyCommand);
    }
}
