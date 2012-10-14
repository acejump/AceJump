package com.johnlindquist.acejump.keycommands;

import com.johnlindquist.acejump.AceFinder;
import com.johnlindquist.acejump.AceJumper;
import com.johnlindquist.acejump.ui.SearchBox;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/24/12
 * Time: 11:54 AM
 */
public class ExpandResults extends AceKeyCommand {
    private final SearchBox searchBox;
    private final AceFinder aceFinder;

    public ExpandResults(SearchBox searchBox, AceFinder aceFinder, AceJumper aceJumper) {
        this.searchBox = searchBox;
        this.aceFinder = aceFinder;
    }

    public void execute(KeyEvent keyEvent) {
        if (searchBox.getText().length() == 0) {

            aceFinder.addResultsReadyListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    eventDispatcher.getMulticaster().stateChanged(new ChangeEvent("ExpandResults"));
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

        eventDispatcher.getMulticaster().stateChanged(new ChangeEvent("ExpandResults"));
    }
}
