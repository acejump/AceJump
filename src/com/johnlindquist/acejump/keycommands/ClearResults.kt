package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.ui.AceCanvas
import java.awt.event.KeyEvent
import com.johnlindquist.acejump.ui.SearchBox

public class ClearResults(val searchBox: SearchBox, val aceCanvas: AceCanvas): AceKeyCommand() {
    override fun execute(keyEvent: KeyEvent) {
        searchBox.setText("");
        aceCanvas.clear()
    }
}