package com.johnlindquist.acejump

import com.intellij.find.FindModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.application.ModalityState.defaultModalityState
import com.intellij.openapi.editor.Editor
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
import com.johnlindquist.acejump.search.Skipper
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.restoreEditorSettings
import com.johnlindquist.acejump.ui.AceUI.setupCursor
import com.johnlindquist.acejump.ui.Canvas
import org.jetbrains.concurrency.runAsync
import java.awt.Color.BLUE
import java.awt.Color.RED
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent.*
import java.lang.System.currentTimeMillis
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

object KeyboardHandler {
  var isEnabled = false
  var redoQueryImmediately = false
  private var text = ""
  private val editorTypeAction = EditorActionManager.getInstance().typedAction
  private val handler = editorTypeAction.rawHandler

  private val keyMap = mutableMapOf(
    VK_HOME to { findPattern(START_OF_LINE) },
    VK_LEFT to { findPattern(START_OF_LINE) },
    VK_RIGHT to { findPattern(END_OF_LINE) },
    VK_END to { findPattern(END_OF_LINE) },
    VK_UP to { findPattern(CODE_INDENTS) },
    VK_ESCAPE to { reset() },
    VK_BACK_SPACE to { processBackspaceCommand() },
    VK_ENTER to { Finder.maybeJumpIfJustOneTagRemains() },
    VK_TAB to { Skipper.ifQueryExistsSkipToNextInEditor(text) }
  )

  private fun findString(string: String) =
    getApplication().invokeLater {
      Finder.findOrJump(FindModel().apply { stringToFind = string });
      updateUIState()
    }

  fun findPattern(pattern: Pattern) =
    getApplication().invokeLater {
      Finder.reset()
      Finder.findOrJump(FindModel().apply {
        stringToFind = pattern.string
        isRegularExpressions = true
      })
      updateUIState()
    }

  fun activate() = getApplication().invokeAndWait({
    if (!isEnabled) startListening() else toggleTargetMode()
  }, defaultModalityState())

  fun processCommand(keyCode: Int) = keyMap[keyCode]?.invoke()

  private fun processBackspaceCommand() {
    text = ""
    Finder.reset()
    updateUIState()
  }

  private val resetListener = object : CaretListener, FocusListener,
    AncestorListener, EditorColorsListener, VisibleAreaListener {
    private val stopWatch = object : () -> Unit {
      val DELAY = 750
      var timer = currentTimeMillis()
      var isRunning = false

      override fun invoke() {
        if (redoQueryImmediately) return redoQuery()

        timer = currentTimeMillis()
        if (isRunning) return
        synchronized(this) {
          isRunning = true

          while (currentTimeMillis() - timer <= DELAY) {
            Thread.sleep(Math.abs(DELAY - (currentTimeMillis() - timer)))
          }

          redoQuery()
          isRunning = false
        }
      }
    }

    override fun visibleAreaChanged(e: VisibleAreaEvent?) {
      if (e!!.isHorizontalScroll()) return
      runAsync(stopWatch)
    }

    fun VisibleAreaEvent.isHorizontalScroll() =
      oldRectangle != newRectangle &&
        oldRectangle.width == newRectangle.width &&
        oldRectangle.height == newRectangle.height &&
        oldRectangle.y == newRectangle.y

    override fun globalSchemeChange(scheme: EditorColorsScheme?) = redoQuery()

    override fun ancestorAdded(event: AncestorEvent?) = reset()

    override fun ancestorMoved(event: AncestorEvent?) = reset()

    override fun ancestorRemoved(event: AncestorEvent?) = reset()

    override fun focusLost(e: FocusEvent?) = reset()

    override fun focusGained(e: FocusEvent?) = reset()

    override fun caretAdded(e: CaretEvent?) = reset()

    override fun caretPositionChanged(e: CaretEvent?) = reset()

    override fun caretRemoved(e: CaretEvent?) = reset()
  }

  fun interceptPrintableKeystrokes() =
    editorTypeAction.setupRawHandler { _, key, _ ->
      text += key
      findString(text)
    }

  private fun configureEditor() =
    editor.run {
      setupCursor()
      Canvas.bindToEditor(this)
      interceptPrintableKeystrokes()
      addListeners()
    }

  private var backup: List<*>? = null

  // Bulenkov: BuildInfo, Graphics2DLog, DrawString/DrawChars, IDEEventQueue.dispatcher
  // This is a grotesque hack to support older IntelliJ Platforms.
  private val ACTIONS_KEY = AnAction::class.java.declaredFields.first {
    it.name == "ACTIONS_KEY" || it.name == "ourClientProperty"
  }.get(null)

  private fun installCustomShortcutHandler() =
    editor.component.run {
      backup = getClientProperty(ACTIONS_KEY) as List<*>?
      putClientProperty(ACTIONS_KEY, SmartList<AnAction>(AceKeyAction))
      val css = CustomShortcutSet(*keyMap.keys.toTypedArray())
      AceKeyAction.registerCustomShortcutSet(css, this)
    }

  private fun uninstallCustomShortCutHandler() =
    editor.component.run {
      putClientProperty(ACTIONS_KEY, backup)
      AceKeyAction.unregisterCustomShortcutSet(this)
    }

  private fun startListening() {
    isEnabled = true
    configureEditor()
    installCustomShortcutHandler()
  }

  private fun updateUIState() =
    if (Jumper.hasJumped) {
      Jumper.hasJumped = false
      reset()
    } else {
      Canvas.jumpLocations = Finder.jumpLocations
      Canvas.repaint()
    }

  fun redoQuery() {
    val tmpText = text
    val tempEnabled = isEnabled
    val tempCache = Finder.sitesToCheck
    reset()
    activate()
    Finder.sitesToCheck = tempCache
    isEnabled = tempEnabled
    text = tmpText
    if (text.isNotEmpty()) findString(text)
  }

  fun resetUIState() {
    text = ""
    isEnabled = false
    redoQueryImmediately = false
    uninstallCustomShortCutHandler()
    editorTypeAction.setupRawHandler(handler)
    Finder.reset()
    restoreEditorSettings()
  }

  fun reset() {
    editor.removeListeners()
    resetUIState()
  }

  private fun Editor.addListeners() =
    synchronized(resetListener) {
      component.addFocusListener(resetListener)
      component.addAncestorListener(resetListener)
      scrollingModel.addVisibleAreaListener(resetListener)
      caretModel.addCaretListener(resetListener)
    }

  private fun Editor.removeListeners() =
    synchronized(resetListener) {
      if (isEnabled) {
        component.removeFocusListener(resetListener)
        component.removeAncestorListener(resetListener)
        scrollingModel.removeVisibleAreaListener(resetListener)
        caretModel.removeCaretListener(resetListener)
      }
    }

  fun toggleTargetMode(status: Boolean? = null) =
    editor.colorsScheme.run {
      if (Finder.toggleTargetMode(status))
        setColor(CARET_COLOR, RED)
      else
        setColor(CARET_COLOR, BLUE)
      Canvas.repaint()
    }
}