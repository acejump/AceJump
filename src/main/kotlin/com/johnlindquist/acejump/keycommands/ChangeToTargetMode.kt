package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.Color
import java.awt.event.KeyEvent
import javax.swing.event.ChangeListener

class ChangeToTargetMode(override val searchBox: SearchBox, override val aceFinder: AceFinder) : AceKeyCommand() {
  init {
    addListener(defaultChangeListener)
  }

  override fun execute(keyEvent: KeyEvent) {
    aceFinder.addResultsReadyListener(ChangeListener {
      eventDispatcher.multicaster.stateChanged(it)
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