package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import javax.swing.event.ChangeEvent

class DefaultKeyCommand(override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) {
    //fixes the delete bug
    if (key == '\b') return

    //Find or jump
    aceFinder.findText(text, key)
    aceFinder.eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
  }
}