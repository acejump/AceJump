package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent

class ShowEndOfLines(override val searchBox: SearchBox, override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(keyEvent: KeyEvent) {
    aceFinder.getEndOffset = true
    aceFinder.findText(AceFinder.END_OF_LINE, true)
    searchBox.forceSpaceChar()
  }
}