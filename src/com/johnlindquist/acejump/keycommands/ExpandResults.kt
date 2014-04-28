package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.AceJumper
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

public class ExpandResults(val searchBox: SearchBox, val aceFinder: AceFinder, aceJumper: AceJumper): AceKeyCommand() {
    override fun execute(keyEvent: KeyEvent) {
        if(searchBox.getText()?.length() == 0){
            aceFinder.addResultsReadyListener(object:ChangeListener{
                public override fun stateChanged(p0: ChangeEvent) {
                    eventDispatcher?.getMulticaster()?.stateChanged(p0);
//                    eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()));
                }
            });

            aceFinder.getEndOffset = true
            aceFinder.findText(AceFinder.CODE_INDENTS, true)

            searchBox.forceSpaceChar()

            return
        }

        if(keyEvent.isShiftDown()){
            aceFinder.contractResults()
        }
        else{
            aceFinder.expandResults()
        }

        eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent(toString()));
    }

}