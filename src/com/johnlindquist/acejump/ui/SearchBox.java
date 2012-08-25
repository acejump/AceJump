package com.johnlindquist.acejump.ui;

import com.intellij.ui.popup.AbstractPopup;
import com.johnlindquist.acejump.keycommands.AceKeyCommand;

import javax.swing.*;
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
    public HashMap preProcessKeyReleasedMap = new HashMap<Integer, AceKeyCommand>();
    public HashMap preProcessKeyPressedMap = new HashMap<Integer, AceKeyCommand>();

    protected boolean isPreProcessed;

    public boolean getIsSearchEnabled() {
        return isSearchEnabled;
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
            isSearchEnabled = true;
        }

        AceKeyCommand aceKeyCommand = null;
        if (keyEvent.getID() == KeyEvent.KEY_RELEASED) {
            aceKeyCommand = (AceKeyCommand) preProcessKeyReleasedMap.get(keyEvent.getKeyCode());
        }

        if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
            aceKeyCommand = (AceKeyCommand) preProcessKeyPressedMap.get(keyEvent.getKeyCode());
        }

        if (aceKeyCommand != null) {
            aceKeyCommand.execute(keyEvent);
            return;
        }

        super.processKeyEvent(keyEvent);
        if (keyEvent.getID() != KeyEvent.KEY_TYPED) return;

        if(defaultKeyCommand != null) defaultKeyCommand.execute(keyEvent);
    }

    public void setIsPreProcessed() {
        isPreProcessed = true;
    }

    public void addSpaceChar() {
        setText(" ");
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
}
