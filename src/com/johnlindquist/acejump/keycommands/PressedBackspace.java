package com.johnlindquist.acejump.keycommands;

import com.johnlindquist.acejump.AceFinder;
import com.johnlindquist.acejump.ui.AceCanvas;
import com.johnlindquist.acejump.ui.SearchBox;

import java.awt.event.KeyEvent;

/**
* Created with IntelliJ IDEA.
* User: johnlindquist
* Date: 8/24/12
* Time: 11:54 AM
* To change this template use File | Settings | File Templates.
*/
public class PressedBackspace extends AceKeyCommand {

    private AceCanvas aceCanvas;

    public PressedBackspace(AceCanvas aceCanvas) {
        this.aceCanvas = aceCanvas;
    }

    public void execute(KeyEvent keyEvent) {
        aceCanvas.clear();
    }
}
