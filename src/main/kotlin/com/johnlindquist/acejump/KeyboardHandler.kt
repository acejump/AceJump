package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.util.SmartList
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
import java.awt.event.FocusEvent
import java.awt.event.FocusListener

object KeyboardHandler {
  @Volatile
  var isEnabled = false
  private var text = ""
  private val handler = EditorActionManager.getInstance().typedAction.rawHandler

  //  val ACTIONS_KEY: Key<List<AnAction>> = Key.create<List<AnAction>>("AnAction.shortcutSet")
  fun activate() = if (!isEnabled) startListening() else toggleTargetMode()

  fun processCommand(keyCode: Int) = keyMap[keyCode]?.invoke()

  fun processBackspaceCommand() {
    text = ""
    Finder.reset()
    updateUIState()
  }

  val resetIfScrollbarChanged = object : VisibleAreaListener {
    override fun visibleAreaChanged(e: VisibleAreaEvent?) {
      editor.scrollingModel.removeVisibleAreaListener(this)
      resetUIState()
    }
  }

  val resetIfCaretPositionChanged = object : CaretListener {
    override fun caretAdded(e: CaretEvent?) {
      caretPositionChanged(e)
    }

    override fun caretPositionChanged(e: CaretEvent?) {
      editor.caretModel.removeCaretListener(this)
      resetUIState()
    }

    override fun caretRemoved(e: CaretEvent?) {
      caretPositionChanged(e)
    }
  }

  val resetIfEditorFocusChanged = object : FocusListener {
    override fun focusGained(e: FocusEvent?) {
    }

    override fun focusLost(e: FocusEvent?) {
      editor.component.removeFocusListener(this)
      resetUIState()
    }
  }

  private fun configureEditor() {
    setupCursor()
    setupCanvas()
    interceptKeystrokes()
    addListeners()
  }

  fun addListeners() {
    editor.scrollingModel.addVisibleAreaListener(resetIfScrollbarChanged)
    editor.caretModel.addCaretListener(resetIfCaretPositionChanged)
    editor.component.addFocusListener(resetIfEditorFocusChanged)
  }

  private fun interceptKeystrokes() {
    EditorActionManager.getInstance().typedAction.setupRawHandler { _, key, _ ->
      text += key
      Finder.findOrJump(text, key)
    }
  }

  private var backup: List<AnAction>? = null

  // This is an grotesque hack.
  private val ACTIONS_KEY = AnAction::class.java.declaredFields.first {
    it.name == "ACTIONS_KEY" || it.name == "ourClientProperty"
  }.get(null)

  private fun configureKeyMap() {
    backup = editor.component.getClientProperty(ACTIONS_KEY) as List<AnAction>?
    val aceActionList = SmartList<AnAction>(AceKeyAction)
    editor.component.putClientProperty(ACTIONS_KEY, aceActionList)
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
    editor.component.putClientProperty(ACTIONS_KEY, backup)
    AceKeyAction.unregisterCustomShortcutSet(editor.component)
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

  fun removeListeners() {
    if (editor.component.focusListeners.contains(resetIfEditorFocusChanged)) {
      editor.scrollingModel.removeVisibleAreaListener(resetIfScrollbarChanged)
      editor.caretModel.removeCaretListener(resetIfCaretPositionChanged)
      editor.component.removeFocusListener(resetIfEditorFocusChanged)
    }
  }
}