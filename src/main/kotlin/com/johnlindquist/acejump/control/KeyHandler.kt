package com.johnlindquist.acejump.control

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
import com.johnlindquist.acejump.config.AceConfig.settings
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Jumper
import com.johnlindquist.acejump.search.Pattern
import com.johnlindquist.acejump.search.Pattern.*
import com.johnlindquist.acejump.search.Skipper
import com.johnlindquist.acejump.view.Canvas
import com.johnlindquist.acejump.view.Model
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.setupCursor
import org.jetbrains.concurrency.runAsync
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent.*
import java.lang.System.currentTimeMillis
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

object KeyHandler {
  private var isEnabled = false
  private var text = ""
  private val editorTypeAction = EditorActionManager.getInstance().typedAction
  private val handler = editorTypeAction.rawHandler
  private var isShiftDown = false
  private val keyMap = mutableMapOf(
    VK_HOME to { findPattern(START_OF_LINE) },
    VK_LEFT to { findPattern(START_OF_LINE) },
    VK_RIGHT to { findPattern(END_OF_LINE) },
    VK_END to { findPattern(END_OF_LINE) },
    VK_UP to { findPattern(CODE_INDENTS) },
    VK_ESCAPE to { reset() },
    VK_BACK_SPACE to { processBackspaceCommand() },
    VK_ENTER to { Finder.maybeJumpIfJustOneTagRemains() },
    VK_TAB to { Skipper.ifQueryExistsSkipToNextInEditor(!isShiftDown) }
  )

  private fun findAgain() = getApplication().invokeLater {
    Finder.find()
    updateUIState()
  }

  private fun findString(string: String) =
    getApplication().invokeLater {
      Finder.findOrJump(FindModel().apply { stringToFind = string })
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
      private val DELAY = 750
      private var timer = currentTimeMillis()
      private var isRunning = false

      override fun invoke() {
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

      fun restart() = runAsync(this)
    }

    override fun visibleAreaChanged(e: VisibleAreaEvent?) {
      if (e!!.isHorizontalScroll()) return
      stopWatch.restart()
    }

    private fun VisibleAreaEvent.isHorizontalScroll() =
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

  private fun interceptPrintableKeystrokes() =
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

  // TODO: Replace this with `BuildInfo.api`, check: idea/146.1211+ ref.:
  // https://github.com/JetBrains/intellij-community/commit/10f16fcd919e4f846930eaa65943e373267f2039#diff-83ee176e2a4882290ced6a65443866cbL70
  private val ACTIONS_KEY = AnAction::class.java.declaredFields.first {
    it.name == "ACTIONS_KEY" || it.name == "ourClientProperty"
  }.get(null)

  // Investigate replacing this with `IDEEventQueue.*Dispatcher(...)`
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
    reset(true)
    activate()

    if (text.isNotEmpty() || Finder.isRegex) findAgain()
  }

  fun reset(redo: Boolean = false) {
    editor.removeListeners()
    isEnabled = false
    uninstallCustomShortCutHandler()
    editorTypeAction.setupRawHandler(handler)
    if (!redo) { text = ""; Finder.reset() }
    restoreEditorSettings()
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
        setColor(CARET_COLOR, settings.targetModeColor)
      else
        setColor(CARET_COLOR, settings.jumpModeColor)
      Canvas.repaint()
    }

  private fun restoreEditorSettings() {
    restoreCanvas()
    restoreCursor()
  }

  private fun restoreCanvas() =
    editor.component.run {
      Canvas.reset()
      remove(Canvas)
      repaint()
    }

  private fun restoreCursor() =
    getApplication().invokeAndWait({
      editor.run {
        settings.isBlinkCaret = Model.naturalBlink
        settings.isBlockCursor = Model.naturalBlock
        colorsScheme.setColor(CARET_COLOR, Model.naturalColor)
      }
    }, defaultModalityState())
}