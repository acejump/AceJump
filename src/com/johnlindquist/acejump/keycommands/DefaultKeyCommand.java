package com.johnlindquist.acejump.keycommands;

import com.johnlindquist.acejump.AceFinder;
import com.johnlindquist.acejump.AceJumper;
import com.johnlindquist.acejump.AceKeyUtil;
import com.johnlindquist.acejump.ui.SearchBox;

import javax.swing.text.BadLocationException;
import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/24/12
 * Time: 11:54 AM
 */
public class DefaultKeyCommand extends AceKeyCommand {
    private SearchBox searchBox;
    private AceFinder aceFinder;
    private AceJumper aceJumper;

    public DefaultKeyCommand(SearchBox searchBox, AceFinder aceFinder, AceJumper aceJumper) {
        this.searchBox = searchBox;
        this.aceFinder = aceFinder;
        this.aceJumper = aceJumper;
    }

    public void execute(KeyEvent keyEvent) {
        char keyChar = keyEvent.getKeyChar();

        if (!searchBox.getIsSearchEnabled()) {
            Integer offset = aceJumper.textAndOffsetHash.get(AceKeyUtil.getLowerCaseStringFromChar(keyChar));
            if (offset != null) {
                searchBox.getPopupContainer().cancel();
                if (keyEvent.isShiftDown()) {
                    aceJumper.setSelectionFromCaretToOffset(offset);
                } else if (keyEvent.isAltDown()) {
                    aceJumper.moveCaret(offset);
                    aceJumper.selectWordAtCaret();
                } else {
                    aceJumper.moveCaret(offset);
                }
            }

        }

        if (searchBox.getIsSearchEnabled() && searchBox.getText().length() == 1) {
            aceFinder.addObserver(new Observer() {
                @Override
                public void update(Observable o, Object arg) {
                    setChanged();
                    notifyObservers();
                }
            });
            aceFinder.findText(searchBox.getText(), false);
            searchBox.disableSearch();
            return;
        }

        if (searchBox.getText().length() == 2) {
            try {
                searchBox.setText(searchBox.getText(0, 1));
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }
    }
}
