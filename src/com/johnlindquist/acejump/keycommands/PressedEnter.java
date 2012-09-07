package com.johnlindquist.acejump.keycommands;

import com.johnlindquist.acejump.AceFinder;
import com.johnlindquist.acejump.AceJumper;
import com.johnlindquist.acejump.ui.SearchBox;

import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/24/12
 * Time: 11:54 AM
 */
public class PressedEnter extends AceKeyCommand {
    private SearchBox searchBox;
    private AceFinder aceFinder;
    private AceJumper aceJumper;

    public PressedEnter(SearchBox searchBox, AceFinder aceFinder, AceJumper aceJumper) {
        this.searchBox = searchBox;
        this.aceFinder = aceFinder;
        this.aceJumper = aceJumper;
    }

    public void execute(KeyEvent keyEvent) {
        if (searchBox.getText().length() == 0) {
            aceFinder.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    setChanged();
                    notifyObservers();
                }
            });
            aceFinder.getEndOffset = true;
            aceFinder.findText(AceFinder.CODE_INDENTS, true);

            searchBox.forceSpaceChar();

            return;
        }


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
