package com.johnlindquist.acejump.keycommands

import com.johnlindquist.acejump.search.AceFinder
import java.awt.event.KeyEvent
import javax.swing.event.ChangeEvent

class DefaultKeyCommand(override val aceFinder: AceFinder) : AceKeyCommand() {
  val aceJumper = AceJumper(aceFinder.editor, aceFinder.document)
  var targetModeEnabled = false

  override fun execute(keyEvent: KeyEvent, text: String) {
    //fixes the delete bug
    if (keyEvent.keyChar == '\b') return

    //Find or jump
    aceFinder.findText(text)
    aceFinder.eventDispatcher.multicaster.stateChanged(ChangeEvent("AceFinder"))
    jumpToOffset(keyEvent, text)
  }

  private fun jumpToOffset(keyEvent: KeyEvent, text: String) {
    val offset = aceFinder.tagMap[text]
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