package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.AceJumper
import com.johnlindquist.acejump.getLowerCaseStringFromChar
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.event.ChangeListener

class DefaultKeyCommand(override val searchBox: SearchBox, override val aceFinder: AceFinder, val aceJumper: AceJumper) : AceKeyCommand() {
    override fun execute(keyEvent: KeyEvent) {
        val keyChar = keyEvent.keyChar

        //fixes the delete bug
        if (keyChar == '\b') return

        //Find or jump
        if (searchBox.isSearchEnabled) {
            //Find
            aceFinder.addResultsReadyListener(ChangeListener { p0 ->
                eventDispatcher?.multicaster?.stateChanged(p0)
                //                    eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()))
            })

            aceFinder.findText(searchBox.text!!, false)
            searchBox.disableSearch()
        } else {
            //Jump to offset!
            var char = getLowerCaseStringFromChar(keyChar)
            if (char == " ") return

            if (aceFinder.firstChar != "") {
                char = aceFinder.firstChar + char
                aceFinder.firstChar = ""
            }
            val offset = textAndOffsetHash[char]

            if (offset != null) {
                searchBox.popupContainer?.cancel()
                if (keyEvent.isShiftDown && !keyEvent.isMetaDown) {
                    aceJumper.setSelectionFromCaretToOffset(offset)
                    aceJumper.moveCaret(offset)
                } else {
                    aceJumper.moveCaret(offset)
                }

                if (aceFinder.isTargetMode) {
                    aceJumper.selectWordAtCaret()
                }
            } else if (textAndOffsetHash.size > 25 && couldPossiblyMatch(char)) {
                aceFinder.firstChar = char
            }

        }

    }

    // we don't want to collect chars which would lead us to nowhere
    private fun couldPossiblyMatch(char: String): Boolean {
        return textAndOffsetHash.keys.any { it.startsWith(char) }
    }


}