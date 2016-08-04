package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent

class ShowBeginningOfLines(override val searchBox: SearchBox, override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(keyEvent: KeyEvent) {
    aceFinder.findText(AceFinder.BEGINNING_OF_LINE, true)
    searchBox.forceSpaceChar()
  }
}