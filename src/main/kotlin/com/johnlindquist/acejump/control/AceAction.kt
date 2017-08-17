package com.johnlindquist.acejump.control

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.project.DumbAwareAction
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.label.Pattern.LINE_MARK
import com.johnlindquist.acejump.view.Model.editor
import java.awt.event.KeyEvent

/**
 * Entry point for all actions. The IntelliJ Platform calls AceJump here.
 */

open class AceAction : DumbAwareAction() {
  override fun update(action: AnActionEvent) {
    action.presentation.isEnabled = action.getData(EDITOR) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    editor = e.getData(EDITOR) ?: editor
    Handler.activate()
  }
}

class AceTargetAction : AceAction() {
  override fun update(action: AnActionEvent) = super.update(action)

  override fun actionPerformed(e: AnActionEvent) =
    super.actionPerformed(e).also { Handler.toggleTargetMode(true) }
}

class AceLineAction : AceAction() {
  override fun update(action: AnActionEvent) = super.update(action)

  override fun actionPerformed(e: AnActionEvent) =
    super.actionPerformed(e).also { Finder.search(LINE_MARK) }
}

object AceKeyAction : AceAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val inputEvent = e.inputEvent as? KeyEvent ?: return
    Handler.processCommand(inputEvent.keyCode)
  }
}