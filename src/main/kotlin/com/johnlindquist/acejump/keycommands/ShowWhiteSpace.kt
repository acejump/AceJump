package com.johnlindquist.acejump.keycommands

import java.awt.event.KeyEvent
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.ui.SearchBox

public class ShowWhiteSpace(val searchBox: SearchBox, val aceFinder: AceFinder): AceKeyCommand() {
    override fun execute(keyEvent: KeyEvent) {
        aceFinder.addResultsReadyListener(object : ChangeListener {
            public override fun stateChanged(p0: ChangeEvent) {
                eventDispatcher?.getMulticaster()?.stateChanged(p0)
//                eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()))
            }
        })

        aceFinder.findText(AceFinder.WHITE_SPACE, true)
        searchBox.forceSpaceChar()
    }
}