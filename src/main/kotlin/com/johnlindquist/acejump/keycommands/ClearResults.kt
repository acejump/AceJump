package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.ui.AceCanvas
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent

class ClearResults(override val searchBox: SearchBox, val aceCanvas: AceCanvas) : AceKeyCommand() {
  override val aceFinder: AceFinder
    get() = throw UnsupportedOperationException()

  override fun execute(keyEvent: KeyEvent) {
    searchBox.text = ""
    aceCanvas.clear()
  }
}