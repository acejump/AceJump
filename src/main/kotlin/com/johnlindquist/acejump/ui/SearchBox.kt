package com.johnlindquist.acejump.ui

import com.intellij.ui.popup.AbstractPopup
import com.johnlindquist.acejump.keycommands.AceKeyCommand
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.JTextField
import javax.swing.text.BadLocationException

class SearchBox: JTextField() {
    val preProcessKeyReleasedMap = HashMap<Int, AceKeyCommand>()
    val preProcessKeyPressedMap = HashMap<Int, AceKeyCommand>()

    var isSearchEnabled: Boolean = true
        get(){
            return field && text?.length == 1
        }

    var popupContainer: AbstractPopup? = null
    var defaultKeyCommand: AceKeyCommand? = null


    override fun requestFocus() {
        transferHandler = null;
        super<JTextField>.requestFocus()
    }
    override fun paintBorder(p0: Graphics?) {
    }


    //todo: I need to really rethink this entire approach
    override fun processKeyEvent(p0: KeyEvent) {
        if (text?.length == 0) {
            //todo: rethink the "isSearchEnabled" state approach. Works great now, could be cleaner
            isSearchEnabled = true
        }

        var aceKeyCommand: AceKeyCommand? = null
        if (p0.id == KeyEvent.KEY_RELEASED) {
            aceKeyCommand = preProcessKeyReleasedMap[p0.keyCode]
        }

        if (p0.id == KeyEvent.KEY_PRESSED) {
            aceKeyCommand = preProcessKeyPressedMap[p0.keyCode]
            //prevent "alt" from triggering menu items
            p0.consume()
        }

        if (aceKeyCommand != null) {
            aceKeyCommand.execute(p0)
            return
        }

        super.processKeyEvent(p0)

        if (p0.id != KeyEvent.KEY_TYPED) return


        if (defaultKeyCommand != null && p0.isConsumed){
            defaultKeyCommand?.execute(p0)
        }

        if (text?.length == 2) {
            try{
                text = getText(0, 1)
            }
            catch (e1: BadLocationException) {
                e1.printStackTrace()
            }
        }
    }

    fun forceSpaceChar() {
        text = " "
        disableSearch()
    }

    fun disableSearch() {
        isSearchEnabled = false
    }

    fun addPreProcessReleaseKey(key: Int, keyCommand: AceKeyCommand) {
        preProcessKeyReleasedMap.put(key, keyCommand);
    }

    fun addPreProcessPressedKey(key: Int, keyCommand: AceKeyCommand) {
        preProcessKeyPressedMap.put(key, keyCommand);
    }

}