package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Pattern.END_OF_LINE

class ShowEndOfLines(override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) =
    aceFinder.findPattern(END_OF_LINE)
}