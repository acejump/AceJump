package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Pattern.LINE_MARK
import com.johnlindquist.acejump.ui.AceUI.document
import com.johnlindquist.acejump.ui.AceUI.editor
import java.awt.event.KeyEvent
import java.util.logging.Logger

var logger = Logger.getLogger("AceKeyAction")
object AceAction : DumbAwareAction() {
  override fun update(action: AnActionEvent) {
    action.presentation.isEnabled = (action.getData(EDITOR)) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    editor = e.getData(EDITOR) as EditorImpl
    document = editor.document.charsSequence.toString().toLowerCase()

    KeyboardHandler.activate()
  }
}

object AceLineAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) = Finder.findPattern(LINE_MARK)
}

object AceKeyAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val inputEvent = e.inputEvent as? KeyEvent ?: return
    logger.info("Seen: " + KeyEvent.getKeyText(inputEvent.keyCode))
    KeyboardHandler.processCommand(inputEvent.keyCode)
  }
}