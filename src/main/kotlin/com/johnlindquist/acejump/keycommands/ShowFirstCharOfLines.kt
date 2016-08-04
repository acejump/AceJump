package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import javax.swing.event.ChangeListener

class ShowFirstCharOfLines(override val searchBox: SearchBox, override val aceFinder: AceFinder) : AceKeyCommand() {
  init {
    addListener(defaultChangeListener)
  }

  override fun execute(keyEvent: KeyEvent) {
    aceFinder.getEndOffset = true
    aceFinder.findText(AceFinder.CODE_INDENTS, true)
    searchBox.forceSpaceChar()
  }
}