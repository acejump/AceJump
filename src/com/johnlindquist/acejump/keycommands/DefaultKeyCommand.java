package com.johnlindquist.acejump.keycommands;

import com.johnlindquist.acejump.AceFinder;
import com.johnlindquist.acejump.AceJumper;
import com.johnlindquist.acejump.AceKeyUtil;
import com.johnlindquist.acejump.ui.SearchBox;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/24/12
 * Time: 11:54 AM
 */
public class DefaultKeyCommand extends AceKeyCommand {
    private final SearchBox searchBox;
    private final AceFinder aceFinder;
    private final AceJumper aceJumper;
    private final HashMap<String, Integer> textAndOffsetHash;

    public DefaultKeyCommand(SearchBox searchBox, AceFinder aceFinder, AceJumper aceJumper, HashMap<String, Integer> textAndOffsetHash) {
        this.searchBox = searchBox;
        this.aceFinder = aceFinder;
        this.aceJumper = aceJumper;
        this.textAndOffsetHash = textAndOffsetHash;
    }

    public void execute(KeyEvent keyEvent) {
        char keyChar = keyEvent.getKeyChar();

        //Find or jump
        if (searchBox.getIsSearchEnabled()) {
            //Find
            aceFinder.addResultsReadyListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    eventDispatcher.getMulticaster().stateChanged(new ChangeEvent("ExpandResults"));
                }
            });


            aceFinder.findText(searchBox.getText(), false);
            searchBox.disableSearch();
        } else {
            //Jump to offset!
            Integer offset = textAndOffsetHash.get(AceKeyUtil.getLowerCaseStringFromChar(keyChar));
            if (offset != null) {
                searchBox.getPopupContainer().cancel();
                if (keyEvent.isShiftDown()) {
                    aceJumper.setSelectionFromCaretToOffset(offset);
                } else {
                    aceJumper.moveCaret(offset);
                }

                if (keyEvent.isAltDown()) {
                    aceJumper.selectWordAtCaret();
                }
            }

        }

    }
}
