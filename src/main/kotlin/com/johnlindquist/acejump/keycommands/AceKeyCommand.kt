package com.johnlindquist.acejump.keycommands

import com.intellij.util.EventDispatcher
import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import javax.swing.event.ChangeListener

abstract class AceKeyCommand {
  abstract val searchBox: SearchBox
  abstract val aceFinder: AceFinder
  open val defaultChangeListener = ChangeListener {
    //Gotta be a nicer way to do this
    searchBox.aceCanvas.jumpInfos = aceFinder.setupJumpLocations()
    searchBox.aceCanvas.repaint()
  }

  open val eventDispatcher: EventDispatcher<ChangeListener> = EventDispatcher.create(ChangeListener::class.java)
  abstract fun execute(keyEvent: KeyEvent)

  open fun addListener(changeListener: ChangeListener) {
    eventDispatcher.addListener(changeListener)
    aceFinder.addResultsReadyListener(ChangeListener {
      eventDispatcher.multicaster.stateChanged(it)
    })
  }
}