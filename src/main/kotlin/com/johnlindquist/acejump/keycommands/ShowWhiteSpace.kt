package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Pattern.WHITE_SPACE

class ShowWhiteSpace(override val aceFinder: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) =
    aceFinder.findPattern(WHITE_SPACE)
}