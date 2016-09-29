package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.getLowerCaseStringFromChar
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.event.ChangeEvent

class DefaultKeyCommand(override val searchBox: SearchBox, override val aceFinder: AceFinder) : AceKeyCommand() {
  val aceJumper = AceJumper(aceFinder.editor, aceFinder.document)

  override fun execute(keyEvent: KeyEvent) {
    val keyChar = keyEvent.keyChar

    //fixes the delete bug
    if (keyChar == '\b') return

    //Find or jump
    aceFinder.findText(searchBox.text, false)
    aceFinder.eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
    //Jump to offset!
    val offset = aceFinder.textAndOffsetHash[searchBox.text]
    if (offset != null) {
      searchBox.popupContainer?.cancel()
      if (keyEvent.isShiftDown && !keyEvent.isMetaDown) {
        aceJumper.setSelectionFromCaretToOffset(offset)
        aceJumper.moveCaret(offset)
      } else {
        aceJumper.moveCaret(offset)
      }

      if (aceFinder.isTargetMode) {
        aceJumper.selectWordAtCaret()
      }
    }

  }

  // we don't want to collect chars which would lead us to nowhere
  private fun couldPossiblyMatch(char: String): Boolean {
    return aceFinder.textAndOffsetHash.keys.any { it.startsWith(char) }
  }
}