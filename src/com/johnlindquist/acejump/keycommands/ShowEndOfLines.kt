package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

public class ShowEndOfLines(val searchBox: SearchBox, val aceFinder: AceFinder): AceKeyCommand() {

    override fun execute(keyEvent: KeyEvent) {
        aceFinder.addResultsReadyListener(object : ChangeListener {
            public override fun stateChanged(p0: ChangeEvent) {
                eventDispatcher?.getMulticaster()?.stateChanged(p0)
//                eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()))
            }
        })

        aceFinder.getEndOffset = true
        aceFinder.findText(AceFinder.END_OF_LINE, true)
        searchBox.forceSpaceChar()
    }
}