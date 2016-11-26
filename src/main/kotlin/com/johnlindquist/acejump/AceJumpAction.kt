package com.johnlindquist.acejump

import com.intellij.find.FindManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.DumbAwareAction
import com.johnlindquist.acejump.search.*
import com.johnlindquist.acejump.search.Pattern.LINE_MARKERS
import com.sun.glass.events.KeyEvent.VK_BACKSPACE
import java.awt.Color.BLUE
import java.awt.Color.RED
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_ESCAPE

open class AceJumpAction : DumbAwareAction() {
  companion object {
    var editor: Editor = getDefaultEditor()
    var project = editor.project!!
    var document = getDocumentFromEditor(editor)
    var findModel = cloneProjectFindModel(project)
    var findManager = FindManager.getInstance(project)
    var naturalCursor = getBlockCursorUserSetting()
    var naturalColor = getNaturalCursorColor()
  }

  var handler: KeyboardHandler? = null

  override fun update(action: AnActionEvent?) {
    action?.presentation?.isEnabled = (action?.getData(EDITOR)) != null
  }

  override fun actionPerformed(actionEvent: AnActionEvent) {
    handler = KeyboardHandler()
    editor = actionEvent.getData(EDITOR) as EditorImpl
    if (!Finder.findEnabled) {
      naturalColor = getNaturalCursorColor()
      naturalCursor = getBlockCursorUserSetting()
      editor.settings.isBlockCursor = true
      naturalCursor = EditorSettingsExternalizable.getInstance().isBlockCursor
      editor.colorsScheme.setColor(CARET_COLOR, BLUE)
    } else {
      if (Finder.toggleTargetMode())
        editor.colorsScheme.setColor(CARET_COLOR, RED)
      else
        editor.colorsScheme.setColor(CARET_COLOR, BLUE)
    }
  }
}

class AceJumpLineAction : AceJumpAction() {
  override fun actionPerformed(actionEvent: AnActionEvent) {
    Finder.findPattern(LINE_MARKERS)
    super.actionPerformed(actionEvent)
  }
}

class AceJumpKeyAction : AceJumpAction() {
  override fun actionPerformed(actionEvent: AnActionEvent) {
    val inputEvent = actionEvent.inputEvent as? KeyEvent ?: return
    when (inputEvent.keyCode) {
      VK_ESCAPE -> returnToNormal()
      VK_BACKSPACE -> handler!!.processBackspaceCommand()
      else -> handler!!.processRegexCommand(inputEvent.keyCode)
    }
  }

  private fun returnToNormal() {
    Finder.reset()
    handler!!.exit()
    editor.settings.isBlockCursor = naturalCursor
    editor.colorsScheme.setColor(CARET_COLOR, naturalColor)
  }
}