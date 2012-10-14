package com.johnlindquist.acejump.keycommands;

import com.intellij.util.EventDispatcher;

import javax.swing.event.ChangeListener;
import java.awt.event.KeyEvent;
import java.util.Observable;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/24/12
 * Time: 11:51 AM
 */
public abstract class AceKeyCommand {
    protected final EventDispatcher<ChangeListener> eventDispatcher = EventDispatcher.create(ChangeListener.class);
    public abstract void execute(KeyEvent keyEvent);

    public void addListener(ChangeListener changeListener){
        eventDispatcher.addListener(changeListener);
    }
}
