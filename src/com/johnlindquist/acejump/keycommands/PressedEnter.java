package com.johnlindquist.acejump.keycommands;

import com.johnlindquist.acejump.AceFinder;
import com.johnlindquist.acejump.ui.SearchBox;

import java.awt.event.KeyEvent;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/24/12
 * Time: 11:54 AM
 */
public class PressedEnter extends AceKeyCommand {
    private SearchBox searchBox;
    private AceFinder aceFinder;

    public PressedEnter(SearchBox searchBox, AceFinder aceFinder) {
        this.searchBox = searchBox;
        this.aceFinder = aceFinder;
    }

    public void execute(KeyEvent keyEvent) {
        if (keyEvent.isShiftDown()) {
            aceFinder.contractResults();
        } else {
            aceFinder.expandResults();
        }
        aceFinder.checkForReset();
        searchBox.setIsPreProcessed();

        setChanged();
        notifyObservers();
    }
}
