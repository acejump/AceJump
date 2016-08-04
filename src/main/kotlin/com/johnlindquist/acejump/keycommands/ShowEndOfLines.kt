package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import javax.swing.event.ChangeListener

class ShowEndOfLines(override val searchBox: SearchBox, override val aceFinder: AceFinder) : AceKeyCommand() {
  init {
    addListener(defaultChangeListener)
  }
  override fun execute(keyEvent: KeyEvent) {
    aceFinder.addResultsReadyListener(ChangeListener { p0 ->
      eventDispatcher.multicaster.stateChanged(p0)
    })

    aceFinder.getEndOffset = true
    aceFinder.findText(AceFinder.END_OF_LINE, true)
    searchBox.forceSpaceChar()
  }
}