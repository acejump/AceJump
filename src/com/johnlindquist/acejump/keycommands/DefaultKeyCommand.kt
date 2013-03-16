package com.johnlindquist.acejump.keycommands

import com.intellij.openapi.util.SystemInfo
import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.AceJumper
import com.johnlindquist.acejump.AceKeyUtil
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import java.util.HashMap
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

public class DefaultKeyCommand(val searchBox: SearchBox, val aceFinder: AceFinder, val aceJumper: AceJumper, val textAndOffsetHash: HashMap<String, Int>): AceKeyCommand() {
    override fun execute(keyEvent: KeyEvent) {
        val keyChar: Char = keyEvent.getKeyChar()

        //Find or jump
        if (searchBox.isSearchEnabled) {
            //Find
            aceFinder.addResultsReadyListener(object: ChangeListener{
                public override fun stateChanged(p0: ChangeEvent) {
                    eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()))
                }
            });

            aceFinder.findText(searchBox.getText()!!, false)
            searchBox.disableSearch()
        } else {
            //Jump to offset!
            val offset = textAndOffsetHash.get(AceKeyUtil.getLowerCaseStringFromChar(keyChar))
            if (offset != null) {
                searchBox.popupContainer?.cancel();
                if (keyEvent.isShiftDown()) {
                    aceJumper.setSelectionFromCaretToOffset(offset)
                } else {
                    aceJumper.moveCaret(offset)
                }

                if (SystemInfo.isMac && keyEvent.isControlDown()) {
                    aceJumper.selectWordAtCaret()
                }

                if (keyEvent.isAltDown()) {
                    aceJumper.selectWordAtCaret()
                }
            }

        }

    }


}