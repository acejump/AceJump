package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Jumper
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.keyMap
import com.johnlindquist.acejump.ui.AceUI.restoreEditorSettings
import com.johnlindquist.acejump.ui.AceUI.setupCanvas
import com.johnlindquist.acejump.ui.Canvas
import com.sun.glass.events.KeyEvent.VK_BACKSPACE
import java.awt.event.KeyEvent.*

object KeyboardHandler {
  @Volatile
  var isEnabled = false
  private var text = ""
  private val handler = EditorActionManager.getInstance().typedAction.rawHandler
  val specials = intArrayOf(VK_BACKSPACE, VK_LEFT, VK_RIGHT, VK_UP, VK_ESCAPE)

  fun processRegexCommand(keyCode: Int) = keyMap[keyCode]?.invoke()

  fun processBackspaceCommand() {
    text = ""
    Finder.reset()
  }

  val returnToNormalIfChanged = VisibleAreaListener { returnToNormal() }
  private fun configureEditor() {
    setupCanvas()
    interceptKeystrokes()
    editor.scrollingModel.addVisibleAreaListener(returnToNormalIfChanged)
  }

  private fun interceptKeystrokes() {
    EditorActionManager.getInstance().typedAction.setupRawHandler { _, key, _ ->
      text += key
      Finder.findOrJump(text, key)
    }
  }

  private fun configureKeyMap() =
    specials.forEach {
      ActionManager.getInstance().getAction("AceKeyAction")
        .registerCustomShortcutSet(it, 0, editor.component)
    }

  fun startListening() {
    configureKeyMap()
    configureEditor()
  }

  fun updateState() {
    if (Jumper.hasJumped) {
      Jumper.hasJumped = false
      returnToNormal()
    } else {
      Canvas.jumpLocations = Finder.jumpLocations
      Canvas.repaint()
    }
  }

  fun returnToNormal() {
    text = ""
    isEnabled = false
    editor.scrollingModel.removeVisibleAreaListener(returnToNormalIfChanged)
    specials.forEach {
      ActionManager.getInstance().getAction("AceKeyAction")
        .unregisterCustomShortcutSet(editor.component)
    }

    EditorActionManager.getInstance().typedAction.setupRawHandler(handler)
    Finder.reset()
    Canvas.reset()
    restoreEditorSettings()
  }
}