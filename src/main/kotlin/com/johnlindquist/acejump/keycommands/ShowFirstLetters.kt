package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Pattern.CODE_INDENTS

class ShowFirstLetters(override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) =
    aceFinder.findPattern(CODE_INDENTS)
}