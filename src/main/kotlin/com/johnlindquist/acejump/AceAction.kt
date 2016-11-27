package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Pattern.LINE_MARKERS
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.setupCursor
import com.johnlindquist.acejump.ui.Canvas
import com.sun.glass.events.KeyEvent.VK_BACKSPACE
import java.awt.Color.RED
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_ESCAPE

open class AceAction : DumbAwareAction() {
  override fun update(action: AnActionEvent?) {
    action?.presentation?.isEnabled = (action?.getData(EDITOR)) != null
  }

  @Synchronized
  override fun actionPerformed(actionEvent: AnActionEvent) {
    editor = actionEvent.getData(EDITOR) as EditorImpl
    setupCursor()
    if (!KeyboardHandler.isEnabled) {
      KeyboardHandler.startListening()
    } else {
      if (Finder.toggleTargetMode())
        editor.colorsScheme.setColor(CARET_COLOR, RED)
      Canvas.repaint()
    }
  }
}

class AceLineAction : AceAction() {
  override fun actionPerformed(actionEvent: AnActionEvent) {
    Finder.findPattern(LINE_MARKERS)
    super.actionPerformed(actionEvent)
  }
}

class AceKeyAction : AceAction() {
  override fun actionPerformed(actionEvent: AnActionEvent) {
    val inputEvent = actionEvent.inputEvent as? KeyEvent ?: return
    when (inputEvent.keyCode) {
      VK_ESCAPE -> KeyboardHandler.returnToNormal()
      VK_BACKSPACE -> KeyboardHandler.processBackspaceCommand()
      else -> KeyboardHandler.processRegexCommand(inputEvent.keyCode)
    }
  }
}