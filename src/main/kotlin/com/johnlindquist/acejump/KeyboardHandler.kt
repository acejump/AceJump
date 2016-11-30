package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Jumper
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.keyMap
import com.johnlindquist.acejump.ui.AceUI.restoreEditorSettings
import com.johnlindquist.acejump.ui.AceUI.setupCanvas
import com.johnlindquist.acejump.ui.AceUI.setupCursor
import com.johnlindquist.acejump.ui.Canvas
import java.awt.Color.BLUE
import java.awt.Color.RED

object KeyboardHandler {
  @Volatile
  var isEnabled = false
  private var text = ""
  private val handler = EditorActionManager.getInstance().typedAction.rawHandler
  private val original: ShortcutSet = AceAction.shortcutSet

  fun activate() {
    if (!KeyboardHandler.isEnabled) {
      KeyboardHandler.startListening()
    } else {
      KeyboardHandler.toggleTargetMode()
    }
  }

  fun processComand(keyCode: Int) = keyMap[keyCode]?.invoke()

  fun processBackspaceCommand() {
    text = ""
    Finder.reset()
    updateUIState()
  }

  val returnToNormalIfChanged = VisibleAreaListener { resetUIState() }
  private fun configureEditor() {
    setupCursor()
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

  private fun configureKeyMap() {
    val css = CustomShortcutSet(*keyMap.keys.toTypedArray())
    AceKeyAction.registerCustomShortcutSet(css, editor.component)
  }

  fun startListening() {
    isEnabled = true
    configureEditor()
    configureKeyMap()
  }

  fun updateUIState() {
    if (Jumper.hasJumped) {
      Jumper.hasJumped = false
      resetUIState()
    } else {
      Canvas.jumpLocations = Finder.jumpLocations
      Canvas.repaint()
    }
  }

  fun resetUIState() {
    text = ""
    isEnabled = false
    AceKeyAction.unregisterCustomShortcutSet(editor.component)
    editor.scrollingModel.removeVisibleAreaListener(returnToNormalIfChanged)
    EditorActionManager.getInstance().typedAction.setupRawHandler(handler)
    Finder.reset()
    Canvas.reset()
    restoreEditorSettings()
  }

  fun toggleTargetMode() {
    if (Finder.toggleTargetMode())
      editor.colorsScheme.setColor(CARET_COLOR, RED)
    else
      editor.colorsScheme.setColor(CARET_COLOR, BLUE)
    Canvas.repaint()
  }
}