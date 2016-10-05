package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Regexp.CODE_INDENTS

class ShowFirstCharOfLines(override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) =
    aceFinder.findText(CODE_INDENTS)
}