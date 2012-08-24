package com.johnlindquist.acejump.keycommands;

import com.johnlindquist.acejump.AceFinder;
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
    private SearchBox searchBox;
    private AceFinder aceFinder;

    public PressedBackspace(SearchBox searchBox, AceFinder aceFinder) {
        this.searchBox = searchBox;
        this.aceFinder = aceFinder;
    }

    public void execute(KeyEvent keyEvent) {
        searchBox.disableSearch();
    }
}
