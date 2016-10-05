package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder

abstract class AceKeyCommand {
  abstract val aceFinder: AceFinder
  abstract fun execute(key: Char = 0.toChar(), text: String = "")
}