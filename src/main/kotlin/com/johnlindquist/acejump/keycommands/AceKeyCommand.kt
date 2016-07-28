package com.johnlindquist.acejump.keycommands

import com.intellij.util.EventDispatcher
import com.johnlindquist.acejump.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import javax.swing.event.ChangeListener

abstract class AceKeyCommand {
    open val eventDispatcher: EventDispatcher<ChangeListener>? = EventDispatcher.create(ChangeListener::class.java)
    abstract val searchBox: SearchBox
    abstract val aceFinder: AceFinder

    abstract fun execute(keyEvent: KeyEvent)

    open fun addListener(changeListener: ChangeListener) {
        eventDispatcher?.addListener(changeListener)
    }
}