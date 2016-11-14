package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.Pattern.*
import javax.swing.event.ChangeEvent

abstract class AceKeyCommand {
  abstract val ace: AceFinder
  abstract fun execute(key: Char = 0.toChar(), text: String = "")
}

class DefaultKeyCommand(override val ace: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) {
    //fixes the delete bug
    if (key == '\b') return

    //Find or jump
    ace.find(text, key)
    ace.eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
  }
}

class ShowWhiteSpace(override val ace: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) = ace.findPattern(WHITE_SPACE)
}

class ShowStartOfLines(override val ace: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) = ace.findPattern(START_OF_LINE)
}

class ShowEndOfLines(override val ace: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) = ace.findPattern(END_OF_LINE)
}

class ShowFirstLetters(override val ace: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) = ace.findPattern(CODE_INDENTS)
}

class ShowLineMarkers(override val ace: AceFinder) : AceKeyCommand() {
  override fun execute(key: Char, text: String) = ace.findPattern(LINE_MARKERS)
}
