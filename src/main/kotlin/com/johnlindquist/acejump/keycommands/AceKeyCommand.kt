package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent

abstract class AceKeyCommand {
  abstract val aceFinder: AceFinder
  abstract fun execute(keyEvent: KeyEvent, text: String)
}