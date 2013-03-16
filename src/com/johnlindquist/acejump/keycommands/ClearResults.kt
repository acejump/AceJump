package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.ui.AceCanvas
import java.awt.event.KeyEvent

public class ClearResults(val aceCanvas:AceCanvas):AceKeyCommand() {
    override fun execute(keyEvent: KeyEvent) {
        aceCanvas.clear()
    }
}