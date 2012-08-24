package com.johnlindquist.acejump.keycommands;

import java.awt.event.KeyEvent;
import java.util.Observable;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/24/12
 * Time: 11:51 AM
 */
public abstract class AceKeyCommand extends Observable {
    public abstract void execute(KeyEvent keyEvent);
}
