package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Regexp
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent

class ShowBeginningOfLines(override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(keyEvent: KeyEvent, text: String) {
    aceFinder.findText(Regexp.BEGINNING_OF_LINE)
  }
}