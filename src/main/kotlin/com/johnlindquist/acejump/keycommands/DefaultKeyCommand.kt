package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import java.awt.event.KeyEvent
import javax.swing.event.ChangeEvent

class DefaultKeyCommand(override val aceFinder: AceFinder) : AceKeyCommand() {

  override fun execute(keyEvent: KeyEvent, text: String) {
    //fixes the delete bug
    if (keyEvent.keyChar == '\b') return

    //Find or jump
    aceFinder.findText(text, keyEvent)
    aceFinder.eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
  }


}