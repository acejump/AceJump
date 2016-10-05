package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Regexp
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent

class ShowEndOfLines(override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) {
    aceFinder.findText(Regexp.END_OF_LINE)
  }
}