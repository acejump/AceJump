package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.AceJumper
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import java.util.HashMap
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

public class ShowFirstAndLast(val searchBox: SearchBox, val aceFinder: AceFinder, val aceJumper: AceJumper, val textAndOffsetHash: HashMap<String, Int>): AceKeyCommand() {
    override fun execute(keyEvent: KeyEvent) {
        aceFinder.addResultsReadyListener(object :ChangeListener{
            public override fun stateChanged(p0: ChangeEvent) {
                eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()))
            }
        })

        if(keyEvent.isMetaDown() || keyEvent.isControlDown()){
            aceFinder.getEndOffset = true
            aceFinder.findText(AceFinder.DEFAULT, true)
            searchBox.forceSpaceChar()
        }
        else{
            val defaultKeyCommand = DefaultKeyCommand(searchBox, aceFinder, aceJumper, textAndOffsetHash)
            defaultKeyCommand.execute(keyEvent)
        }
    }
}