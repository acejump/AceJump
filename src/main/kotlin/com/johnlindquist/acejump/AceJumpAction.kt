package com.johnlindquist.acejump

import com.intellij.find.FindManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.johnlindquist.acejump.keycommands.ShowLineMarkers
import com.johnlindquist.acejump.search.AceFinder
import com.sun.glass.events.KeyEvent.VK_BACKSPACE
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_ESCAPE

private var aceFinder: AceFinder? = null
private var searchBox: KeyboardHandler? = null

open class AceJumpAction() : DumbAwareAction() {
  override fun update(e: AnActionEvent?) {
    e?.presentation?.isEnabled = (e?.getData(EDITOR)) != null
  }

  override fun actionPerformed(actionEvent: AnActionEvent) {
    // Todo: find a way to avoid re-instantiating these on each invocation
    val project = actionEvent.getData(PROJECT) as Project
    val editor = actionEvent.getData(EDITOR) as EditorImpl
    val findManager = FindManager.getInstance(project)!!
    aceFinder = AceFinder(findManager, editor)
    searchBox = KeyboardHandler(aceFinder!!, editor)
  }
}

class AceJumpLineAction : AceJumpAction() {
  override fun actionPerformed(actionEvent: AnActionEvent) {
    searchBox!!.processRegexCommand(ShowLineMarkers(aceFinder!!))
    super.actionPerformed(actionEvent)
  }
}

class AceJumpKeyAction : AceJumpAction() {
  override fun actionPerformed(actionEvent: AnActionEvent) {
    val inputEvent = actionEvent.inputEvent as? KeyEvent ?: return

    when (inputEvent.keyCode) {
      VK_ESCAPE -> searchBox!!.exit()
      VK_BACKSPACE -> searchBox!!.processBackspaceCommand()
      else -> searchBox!!.processRegexCommand(inputEvent.keyCode)
    }
  }
}