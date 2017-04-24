package com.johnlindquist.acejump

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.util.SmartList
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Jumper
import com.johnlindquist.acejump.search.Pattern
import com.johnlindquist.acejump.search.Pattern.*
import com.johnlindquist.acejump.search.Pattern.Companion.REGEX_PREFIX
import com.johnlindquist.acejump.search.getDefaultEditor
import com.johnlindquist.acejump.ui.AceUI
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
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

object KeyboardHandler {
  var isEnabled = false
  private var text = ""
  private val editorActionManager = EditorActionManager.getInstance()
  private val handler = editorActionManager.typedAction.rawHandler

  private val keyMap = mutableMapOf(
    VK_HOME to { findPattern(START_OF_LINE) },
    VK_LEFT to { findPattern(START_OF_LINE) },
    VK_RIGHT to { findPattern(END_OF_LINE) },
    VK_END to { findPattern(END_OF_LINE) },
    VK_UP to { findPattern(CODE_INDENTS) },
    VK_ESCAPE to { reset() },
    VK_BACK_SPACE to { processBackspaceCommand() }
  )

  private fun findString(query: String, key: Char) {
    getApplication().runReadAction { Finder.findOrJump(query, key) }
    getApplication().invokeLater { updateUIState() }
  }

  fun findPattern(pattern: Pattern) {
    Finder.reset()
    findModel.isRegularExpressions = true
    // TODO: Fix this really bad hack.
    findString(pattern.pattern, REGEX_PREFIX)
  }

  fun activate() = if (!isEnabled) startListening() else toggleTargetMode()

  fun processCommand(keyCode: Int) = keyMap[keyCode]?.invoke()

  private fun processBackspaceCommand() {
    text = ""
    Finder.reset()
    updateUIState()
  }

  private val resetListener = object : CaretListener, VisibleAreaListener,
    FocusListener, AncestorListener, EditorColorsListener {
    override fun globalSchemeChange(scheme: EditorColorsScheme?) = redoQuery()

    override fun ancestorAdded(event: AncestorEvent?) = reset()

    override fun ancestorMoved(event: AncestorEvent?) = reset()

    override fun ancestorRemoved(event: AncestorEvent?) = reset()

    override fun visibleAreaChanged(e: VisibleAreaEvent?) = redoQuery()

    override fun focusLost(e: FocusEvent?) = reset()

    override fun focusGained(e: FocusEvent?) = reset()

    override fun caretAdded(e: CaretEvent?) = reset()

    override fun caretPositionChanged(e: CaretEvent?) = reset()

    override fun caretRemoved(e: CaretEvent?) = reset()
  }

  private fun configureEditor() {
    fun interceptPrintableKeystrokes() {
      editorActionManager.typedAction.setupRawHandler { _, key, _ ->
        text += key
        findString(text, key)
      }
    }

    editor = getDefaultEditor()
    setupCursor()
    setupCanvas()
    interceptPrintableKeystrokes()
    addListeners()
  }

  private var backup: List<*>? = null

  // Bulenkov: BuildInfo, Graphics2DLog, DrawString/DrawChars, IDEEventQueue.dispatcher
  // This is a grotesque hack to support older IntelliJ Platforms.
  private val ACTIONS_KEY = AnAction::class.java.declaredFields.first {
    it.name == "ACTIONS_KEY" || it.name == "ourClientProperty"
  }.get(null)

  private fun installCustomShortcutHandler() {
    backup = editor.component.getClientProperty(ACTIONS_KEY) as List<*>?
    val aceActionList = SmartList<AnAction>(AceKeyAction)
    editor.component.putClientProperty(ACTIONS_KEY, aceActionList)
    val css = CustomShortcutSet(*keyMap.keys.toTypedArray())
    AceKeyAction.registerCustomShortcutSet(css, editor.component)
  }

  private fun startListening() {
    fun startup() {
      configureEditor()
      installCustomShortcutHandler()
    }

    isEnabled = true
    startup()
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

  fun redoQuery() {
    val tmpText = text
    reset()
    activate()
    text = tmpText
    if (text.isNotEmpty()) {
      findString(text, text.last())
    }
  }

  fun reset() {
    fun resetUIState() {
      text = ""
      isEnabled = false
      editor.component.putClientProperty(ACTIONS_KEY, backup)
      AceKeyAction.unregisterCustomShortcutSet(editor.component)
      editorActionManager.typedAction.setupRawHandler(handler)
      Finder.reset()
      restoreEditorSettings()
    }

    removeListeners()
    resetUIState()
  }

  private fun addListeners() {
    synchronized(resetListener) {
      editor.component.addFocusListener(resetListener)
      editor.component.addAncestorListener(resetListener)
      editor.scrollingModel.addVisibleAreaListener(resetListener)
      editor.caretModel.addCaretListener(resetListener)
    }
  }

  private fun removeListeners() {
    synchronized(resetListener) {
      if (isEnabled) {
        editor.component.removeFocusListener(resetListener)
        editor.component.removeAncestorListener(resetListener)
        editor.scrollingModel.removeVisibleAreaListener(resetListener)
        editor.caretModel.removeCaretListener(resetListener)
      }
    }
  }

  fun toggleTargetMode(status: Boolean? = null) {
    if (Finder.toggleTargetMode(status))
      editor.colorsScheme.setColor(CARET_COLOR, RED)
    else
      editor.colorsScheme.setColor(CARET_COLOR, BLUE)
    Canvas.repaint()
  }
}