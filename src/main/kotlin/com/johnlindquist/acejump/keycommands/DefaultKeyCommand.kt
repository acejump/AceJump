package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import com.johnlindquist.acejump.search.getLowerCaseStringFromChar
import com.johnlindquist.acejump.ui.SearchBox
import java.awt.Color
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.event.ChangeEvent

class DefaultKeyCommand(override val aceFinder: AceFinder) : AceKeyCommand() {
  val aceJumper = AceJumper(aceFinder.editor, aceFinder.document)
  var targetModeEnabled = false

  override fun execute(keyEvent: KeyEvent, text: String) {
    //fixes the delete bug
    if (keyEvent.keyChar == '\b') return

    //Find or jump
    aceFinder.findText(text, false)
    aceFinder.eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
    jumpToOffset(keyEvent, text)
  }

  private fun jumpToOffset(keyEvent: KeyEvent, text: String) {
    val offset = aceFinder.textAndOffsetHash[text]
    if (offset != null) {
      if (keyEvent.isShiftDown && !keyEvent.isMetaDown) {
        aceJumper.setSelectionFromCaretToOffset(offset)
        aceJumper.moveCaret(offset)
      } else {
        aceJumper.moveCaret(offset)
      }

      if (targetModeEnabled) {
        aceJumper.selectWordAtCaret()
      }
    }
  }

  fun toggleTargetMode(): Boolean {
    targetModeEnabled = !targetModeEnabled
    return targetModeEnabled
  }
}