package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.REGEX
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent

class ShowWhiteSpace(override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(keyEvent: KeyEvent, text: String) {
    aceFinder.findText(REGEX.WHITE_SPACE)
  }
}