package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Regexp.BEGINNING_OF_LINE

class ShowStartOfLines(override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) =
    aceFinder.findText(BEGINNING_OF_LINE)
}