package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Pattern.LINE_MARK
import com.johnlindquist.acejump.ui.AceUI.document
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.setupCursor
import com.johnlindquist.acejump.ui.Canvas
import com.sun.glass.events.KeyEvent.VK_BACKSPACE
import java.awt.Color.BLUE
import java.awt.Color.RED
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_ESCAPE

open class AceAction : DumbAwareAction() {
  override fun update(action: AnActionEvent) {
    action.presentation.isEnabled = (action.getData(EDITOR)) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    editor = e.getData(EDITOR) as EditorImpl
    document = editor.document.charsSequence.toString().toLowerCase()
    if (!KeyboardHandler.isEnabled) {
      KeyboardHandler.isEnabled = true
      setupCursor()
      KeyboardHandler.startListening()
    } else {
      if (Finder.toggleTargetMode())
        editor.colorsScheme.setColor(CARET_COLOR, RED)
      else
        editor.colorsScheme.setColor(CARET_COLOR, BLUE)
      Canvas.repaint()
    }
  }
}

class AceLineAction : AceAction() {
  override fun actionPerformed(e: AnActionEvent) = Finder.findPattern(LINE_MARK)
}

class AceKeyAction : AceAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val inputEvent = e.inputEvent as? KeyEvent ?: return
    when (inputEvent.keyCode) {
      VK_ESCAPE -> KeyboardHandler.returnToNormal()
      VK_BACKSPACE -> KeyboardHandler.processBackspaceCommand()
      else -> KeyboardHandler.processRegexCommand(inputEvent.keyCode)
    }
  }
}