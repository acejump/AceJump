package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Regexp.WHITE_SPACE

class ShowWhiteSpace(override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) = aceFinder.findText(WHITE_SPACE)
}