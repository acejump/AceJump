package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.Color
import java.awt.event.KeyEvent
import javax.swing.event.ChangeListener

class ChangeToTargetMode(override val searchBox: SearchBox, val aceFinder: AceFinder) : AceKeyCommand() {
    override fun execute(keyEvent: KeyEvent) {
        aceFinder.addResultsReadyListener(ChangeListener { p0 ->
            eventDispatcher?.multicaster?.stateChanged(p0)
            //                eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()))
        })

        if (keyEvent.isMetaDown || keyEvent.isControlDown) {
            if (aceFinder.isTargetMode) {
                aceFinder.isTargetMode = false
                searchBox.background = Color.WHITE
            } else {
                aceFinder.isTargetMode = true
                searchBox.background = Color.RED
            }
        }
    }
}