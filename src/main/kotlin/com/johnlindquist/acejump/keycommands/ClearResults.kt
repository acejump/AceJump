package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.ui.AceCanvas
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent

class ClearResults(val searchBox: SearchBox, val aceCanvas: AceCanvas): AceKeyCommand() {
    override fun execute(keyEvent: KeyEvent) {
        searchBox.text = "";
        aceCanvas.clear()
    }
}