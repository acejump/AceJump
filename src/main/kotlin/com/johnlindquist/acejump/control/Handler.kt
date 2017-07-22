package com.johnlindquist.acejump.control

import com.intellij.find.FindModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CustomShortcutSet
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
import com.johnlindquist.acejump.config.AceConfig.Companion.settings
import com.johnlindquist.acejump.search.*
import com.johnlindquist.acejump.search.Pattern.*
import com.johnlindquist.acejump.view.Canvas
import com.johnlindquist.acejump.view.Model
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.setupCursor
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import kotlin.system.measureTimeMillis

/**
 * Handles all incoming keystrokes, IDE notifications, and UI updates.
 */

object Handler {
  private var enabled = false
  private var text = ""
  private var range = 0..0
  private var scrollX = 0
  private var scrollY = 0
  private val editorTypeAction = EditorActionManager.getInstance().typedAction
  private val handler = editorTypeAction.rawHandler
  private var isShiftDown = false
  private val keyMap = mutableMapOf(
    VK_HOME to { START_OF_LINE.find() },
    VK_LEFT to { START_OF_LINE.find() },
    VK_RIGHT to { END_OF_LINE.find() },
    VK_END to { END_OF_LINE.find() },
    VK_UP to { CODE_INDENTS.find() },
    VK_ESCAPE to { reset() },
    VK_BACK_SPACE to { processBackspaceCommand() },
    VK_ENTER to { Finder.maybeJumpIfJustOneTagRemains() },
    VK_TAB to { Skipper.ifQueryExistsSkipToNextInEditor(!isShiftDown) }
  )

  private fun String.find(skim: Boolean = false) =
    runLater {
      Finder.findOrJump(FindModel().apply { stringToFind = this@find }, skim)
      updateUIState()
    }

  fun findPattern(pattern: Pattern) = pattern.find()

  private fun Pattern.find() =
    runLater {
      Finder.reset()
      Finder.findOrJump(FindModel().apply {
        stringToFind = string
        isRegularExpressions = true
      })
      updateUIState()
    }

  fun activate() = runNow { if (!enabled) start() else toggleTargetMode() }

  fun processCommand(keyCode: Int) = keyMap[keyCode]?.invoke()

  private fun processBackspaceCommand() {
    text = ""
    Finder.reset()
    updateUIState()
  }

  private val resetListener = object : CaretListener, FocusListener,
    AncestorListener, EditorColorsListener, VisibleAreaListener {
    override fun visibleAreaChanged(e: VisibleAreaEvent?) {
      val elapsed = measureTimeMillis { if (canSurviveViewAdjustment()) return }
      Trigger.restart(delay = (750L - elapsed).coerceAtLeast(0L)) { redoFind() }
    }

    private fun canSurviveViewAdjustment(): Boolean =
      editor.getView().run {
        if (first in range && last in range) return true
        else Finder.sitesToCheck.hasTagBetweenOldAndNewViewTop(range, this)
          && Finder.sitesToCheck.hasTagBetweenOldAndNewViewBottom(range, this)
      }

    override fun globalSchemeChange(scheme: EditorColorsScheme?) = redoFind()

    override fun ancestorAdded(event: AncestorEvent?) = reset()

    override fun ancestorMoved(event: AncestorEvent?) =
      if (canSurviveViewAdjustment()) Unit else reset()

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
      if (text.length < 2) {
        text.find(skim = true)
        Trigger.restart(400L) { text.find() }
      } else text.find()
    }

  private fun configureEditor() =
    editor.run {
      setupCursor()
      range = getView()
      scrollX = scrollingModel.horizontalScrollOffset
      scrollY = scrollingModel.verticalScrollOffset
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

  private fun start() {
    enabled = true
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

  fun redoFind() {
    runNow { restoreCanvas(); Canvas.bindToEditor(editor) }
    if (text.isNotEmpty() || Finder.isRegex)
      runLater {
        Finder.find()
        updateUIState()
      }
  }

  fun reset() {
    editor.removeListeners()
    enabled = false
    uninstallCustomShortCutHandler()
    editorTypeAction.setupRawHandler(handler)
    text = ""
    Finder.reset()
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
      if (enabled) {
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
    restoreScroll()
    restoreCanvas()
    restoreCursor()
  }

  private fun restoreScroll() = editor.scrollingModel.scroll(scrollX, scrollY)

  private fun restoreCanvas() =
    editor.component.run {
      Canvas.reset()
      remove(Canvas)
      repaint()
    }

  private fun restoreCursor() = runNow {
    editor.run {
      settings.isBlinkCaret = Model.naturalBlink
      settings.isBlockCursor = Model.naturalBlock
      colorsScheme.setColor(CARET_COLOR, Model.naturalColor)
    }
  }
}