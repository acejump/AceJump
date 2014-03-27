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

        //fixes the delete bug
        if (keyChar == '\b') return
        println(searchBox.isSearchEnabled)
        //Find or jump
        if (searchBox.isSearchEnabled) {
            //Find
            aceFinder.addResultsReadyListener(object: ChangeListener{
                public override fun stateChanged(p0: ChangeEvent) {
                    eventDispatcher?.getMulticaster()?.stateChanged(p0)
//                    eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()))
                }
            })

            aceFinder.findText(searchBox.getText()!!, false)
            searchBox.disableSearch()
        } else {
            //Jump to offset!
            var char = AceKeyUtil.getLowerCaseStringFromChar(keyChar)
            if(char == " ") return

            if(aceFinder.firstChar != ""){
                char = aceFinder.firstChar + char
                aceFinder.firstChar = ""
            }
            val offset = textAndOffsetHash.get(char)

            if (offset != null) {
                searchBox.popupContainer?.cancel();
                if (keyEvent.isShiftDown() && !keyEvent.isMetaDown()) {
                    aceJumper.setSelectionFromCaretToOffset(offset)
                    aceJumper.moveCaret(offset)
                } else {
                    aceJumper.moveCaret(offset)
                }

                if (aceFinder.isTargetMode) {
                    aceJumper.selectWordAtCaret()
                }
            }
            else if(textAndOffsetHash.size > 25){
                aceFinder.firstChar = char!!
            }

        }

    }


}