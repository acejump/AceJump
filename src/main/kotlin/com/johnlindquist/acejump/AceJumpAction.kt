package com.johnlindquist.acejump

import com.intellij.find.FindManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColorsManager.getInstance
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.johnlindquist.acejump.keycommands.ShowLineMarkers
import com.johnlindquist.acejump.search.AceFinder
import com.sun.glass.events.KeyEvent.VK_BACKSPACE
import java.awt.Color
import java.awt.Color.RED
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_ESCAPE

private var aceFinder: AceFinder? = null
private var keyboardHandler: KeyboardHandler? = null
val isBlockCursor = EditorSettingsExternalizable.getInstance().isBlockCursor
val naturalColor: Color = getInstance().globalScheme.getColor(CARET_COLOR)!!

open class AceJumpAction() : DumbAwareAction() {
  override fun update(e: AnActionEvent?) {
    e?.presentation?.isEnabled = (e?.getData(EDITOR)) != null
  }

  override fun actionPerformed(actionEvent: AnActionEvent) {
    // Todo: find a way to avoid re-instantiating these on each invocation
    val project = actionEvent.getData(PROJECT) as Project
    val editor = actionEvent.getData(EDITOR) as EditorImpl
    val findManager = FindManager.getInstance(project)!!
    if (aceFinder == null) {
      editor.settings.isBlockCursor = true
      aceFinder = AceFinder(findManager, editor)
      keyboardHandler = KeyboardHandler(aceFinder!!, editor)
    } else {
      if (aceFinder!!.toggleTargetMode()) {
        editor.colorsScheme.setColor(CARET_COLOR, RED)
      } else {
        editor.colorsScheme.setColor(CARET_COLOR, naturalColor)
      }
    }
  }
}

class AceJumpLineAction : AceJumpAction() {
  override fun actionPerformed(actionEvent: AnActionEvent) {
    keyboardHandler!!.processRegexCommand(ShowLineMarkers(aceFinder!!))
    super.actionPerformed(actionEvent)
  }
}

class AceJumpKeyAction : AceJumpAction() {
  override fun actionPerformed(actionEvent: AnActionEvent) {
    val inputEvent = actionEvent.inputEvent as? KeyEvent ?: return
    val editor = actionEvent.getData(EDITOR) as EditorImpl
    when (inputEvent.keyCode) {
      VK_ESCAPE -> { keyboardHandler!!.exit()
        aceFinder = null
        editor.settings.isBlockCursor = isBlockCursor
      }
      VK_BACKSPACE -> keyboardHandler!!.processBackspaceCommand()
      else -> keyboardHandler!!.processRegexCommand(inputEvent.keyCode)
    }
  }
}