package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.util.SmartList
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Jumper
import com.johnlindquist.acejump.search.Pattern
import com.johnlindquist.acejump.search.Pattern.*
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.findModel
import com.johnlindquist.acejump.ui.AceUI.restoreEditorSettings
import com.johnlindquist.acejump.ui.AceUI.setupCanvas
import com.johnlindquist.acejump.ui.AceUI.setupCursor
import com.johnlindquist.acejump.ui.Canvas
import java.awt.Color.BLUE
import java.awt.Color.RED
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent.*

object KeyboardHandler {
  @Volatile
  var isEnabled = false
  private var text = ""
  private val handler = EditorActionManager.getInstance().typedAction.rawHandler

  private val keyMap = mutableMapOf(
    VK_HOME to { find(START_OF_LINE) },
    VK_LEFT to { find(START_OF_LINE) },
    VK_RIGHT to { find(END_OF_LINE) },
    VK_END to { find(END_OF_LINE) },
    VK_UP to { find(CODE_INDENTS) },
    VK_ESCAPE to { resetUIState() },
    VK_BACK_SPACE to { processBackspaceCommand() }
  )

  private fun findAndUpdateUI(query: String, key: Char) {
    getApplication().runReadAction { Finder.findOrJump(query, key) }
    getApplication().invokeLater { updateUIState() }
  }

  fun find(pattern: Pattern) {
    Finder.reset()
    findModel.isRegularExpressions = true
    findAndUpdateUI(pattern.pattern, Pattern.REGEX_PREFIX)
  }

  fun activate() = if (!isEnabled) startListening() else toggleTargetMode()

  fun processCommand(keyCode: Int) = keyMap[keyCode]?.invoke()

  private fun processBackspaceCommand() {
    text = ""
    Finder.reset()
    updateUIState()
  }

  private val resetIfScrollbarChanged = VisibleAreaListener { reset() }

  private val resetIfCaretPositionChanged = object : CaretListener {
    override fun caretAdded(e: CaretEvent?) = reset()

    override fun caretPositionChanged(e: CaretEvent?) = reset()

    override fun caretRemoved(e: CaretEvent?) = reset()
  }

  private val resetIfEditorFocusChanged = object : FocusListener {
    override fun focusGained(e: FocusEvent?) = reset()

    override fun focusLost(e: FocusEvent?) = reset()
  }

  private fun configureEditor() {
    setupCursor()
    setupCanvas()
    interceptPrintableKeystrokes()
    addListeners()
  }

  private fun addListeners() {
    synchronized(resetIfEditorFocusChanged) {
      editor.scrollingModel.addVisibleAreaListener(resetIfScrollbarChanged)
      editor.caretModel.addCaretListener(resetIfCaretPositionChanged)
      editor.component.addFocusListener(resetIfEditorFocusChanged)
    }
  }

  private fun interceptPrintableKeystrokes() {
    EditorActionManager.getInstance().typedAction.setupRawHandler { _, key, _ ->
      text += key
      findAndUpdateUI(text, key)
    }
  }

  private var backup: List<AnAction>? = null
  // This is a grotesque hack to support older IntelliJ Platforms.
  private val ACTIONS_KEY = AnAction::class.java.declaredFields.first {
    it.name == "ACTIONS_KEY" || it.name == "ourClientProperty"
  }.get(null)

  private fun installCustomShortcutHandler() {
    backup = editor.component.getClientProperty(ACTIONS_KEY) as List<AnAction>?
    val aceActionList = SmartList<AnAction>(AceKeyAction)
    editor.component.putClientProperty(ACTIONS_KEY, aceActionList)
    val css = CustomShortcutSet(*keyMap.keys.toTypedArray())
    AceKeyAction.registerCustomShortcutSet(css, editor.component)
  }

  private fun startListening() {
    isEnabled = true
    configureEditor()
    installCustomShortcutHandler()
  }

  private fun updateUIState() {
    if (Jumper.hasJumped) {
      Jumper.hasJumped = false
      reset()
    } else {
      Canvas.jumpLocations = Finder.jumpLocations
      Canvas.repaint()
    }
  }

  fun reset() {
    removeListeners()
    resetUIState()
  }

  private fun removeListeners() {
    synchronized(resetIfEditorFocusChanged) {
      if (editor.component.focusListeners.contains(resetIfEditorFocusChanged)) {
        editor.component.removeFocusListener(resetIfEditorFocusChanged)
        editor.scrollingModel.removeVisibleAreaListener(resetIfScrollbarChanged)
        editor.caretModel.removeCaretListener(resetIfCaretPositionChanged)
      }
    }
  }

  private fun resetUIState() {
    text = ""
    isEnabled = false
    editor.component.putClientProperty(ACTIONS_KEY, backup)
    AceKeyAction.unregisterCustomShortcutSet(editor.component)
    EditorActionManager.getInstance().typedAction.setupRawHandler(handler)
    Finder.reset()
    Canvas.reset()
    restoreEditorSettings()
  }

  fun toggleTargetMode(status: Boolean? = null) {
    if (Finder.toggleTargetMode(status))
      editor.colorsScheme.setColor(CARET_COLOR, RED)
    else
      editor.colorsScheme.setColor(CARET_COLOR, BLUE)
    Canvas.repaint()
  }
}