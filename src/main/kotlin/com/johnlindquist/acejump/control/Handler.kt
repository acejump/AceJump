package com.johnlindquist.acejump.control

import com.intellij.find.FindModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
import com.intellij.util.SmartList
import com.johnlindquist.acejump.config.AceConfig.Companion.settings
import com.johnlindquist.acejump.search.*
import com.johnlindquist.acejump.search.Pattern.*
import com.johnlindquist.acejump.search.Skipper.restoreScroll
import com.johnlindquist.acejump.search.Skipper.storeScroll
import com.johnlindquist.acejump.view.Canvas
import com.johnlindquist.acejump.view.Model
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.setupCursor
import com.johnlindquist.acejump.view.Model.viewBounds
import java.awt.event.KeyEvent.*
import javax.swing.JComponent

/**
 * Handles all incoming keystrokes, IDE notifications, and UI updates.
 */

object Handler {
  private var enabled = false
  private var text = ""
  private val editorTypeAction = EditorActionManager.getInstance().typedAction
  private val handler = editorTypeAction.rawHandler
  private var isShiftDown = false
  private val keyMap = mapOf(
    VK_HOME to { findPattern(START_OF_LINE) },
    VK_LEFT to { findPattern(START_OF_LINE) },
    VK_RIGHT to { findPattern(END_OF_LINE) },
    VK_END to { findPattern(END_OF_LINE) },
    VK_UP to { findPattern(CODE_INDENTS) },
    VK_ESCAPE to { reset() },
    VK_BACK_SPACE to { processBackspaceCommand() },
    VK_ENTER to { Finder.maybeJumpIfJustOneTagRemains() },
    VK_TAB to { Skipper.doesQueryExistIfSoSkipToIt(!isShiftDown) }
  )

  private fun findOrDropLast() =
    if (!Finder.isQueryDeadEnd(text)) {
      find(text)
    } else {
      text = text.dropLast(1)
    }

  private fun find(key: String, doSkim: Boolean = false) =
    Highlighter.search(FindModel().apply { stringToFind = key; skim = doSkim })

  fun findPattern(pattern: Pattern) =
    Highlighter.search(FindModel().apply {
      stringToFind = pattern.string
      isRegularExpressions = true
      skim = false
      Finder.reset()
    })

  fun activate() = runNow { if (!enabled) start() else toggleTargetMode() }

  fun processCommand(keyCode: Int) = keyMap[keyCode]?.invoke()

  private fun processBackspaceCommand() {
    text = ""
    Finder.reset()
    Highlighter.discard()
    updateUIState()
  }

  private fun interceptPrintableKeystrokes() =
    editorTypeAction.setupRawHandler { _, key, _ ->
      text += key
      if (text.length < 2) {
        find(text, doSkim = true)
        Trigger.restart(400L) { find(text, doSkim = false) }
      } else findOrDropLast()
    }

  private fun configureEditor() =
    editor.run {
      storeScroll()
      setupCursor()
      viewBounds = getView()
      Canvas.bindToEditor(this)
      interceptPrintableKeystrokes()
      Listener.enable()
    }

  private var backup: List<*>? = null

  // TODO: Replace this with `BuildInfo.api`, check: idea/146.1211+ ref.:
  // https://github.com/JetBrains/intellij-community/commit/10f16fcd919e4f846930eaa65943e373267f2039#diff-83ee176e2a4882290ced6a65443866cbL70
  private val ACTIONS_KEY = AnAction::class.java.declaredFields.first {
    it.name == "ACTIONS_KEY" || it.name == "ourClientProperty"
  }.get(null)

  // Investigate replacing this with `IDEEventQueue.*Dispatcher(...)`
  private fun JComponent.installCustomShortcutHandler() {
    backup = getClientProperty(ACTIONS_KEY) as List<*>?
    putClientProperty(ACTIONS_KEY, SmartList<AnAction>(AceKeyAction))
    val css = CustomShortcutSet(*keyMap.keys.toTypedArray())
    AceKeyAction.registerCustomShortcutSet(css, this)
  }

  private fun JComponent.uninstallCustomShortCutHandler() {
    putClientProperty(ACTIONS_KEY, backup)
    AceKeyAction.unregisterCustomShortcutSet(this)
  }

  private fun start() {
    enabled = true
    configureEditor()
    editor.component.installCustomShortcutHandler()
  }

  fun updateUIState() =
    if (Jumper.hasJumped) {
      Jumper.hasJumped = false
      reset()
    } else {
      Canvas.jumpLocations = Finder.markers
      Canvas.repaint()
    }

  fun redoFind() {
    runNow {
      editor.restoreCanvas()
      Canvas.bindToEditor(editor)
    }

    if (text.isNotEmpty() || Finder.isRegex)
      runLater {
        Finder.find()
        updateUIState()
      }
  }

  fun reset() {
    if (enabled) Listener.disable()
    editor.component.uninstallCustomShortCutHandler()
    editorTypeAction.setupRawHandler(handler)
    enabled = false
    text = ""
    Finder.reset()
    Highlighter.discard()
    editor.restoreSettings()
  }

  fun toggleTargetMode(status: Boolean? = null) =
    editor.colorsScheme.run {
      if (Finder.toggleTargetMode(status))
        setColor(CARET_COLOR, settings.targetModeColor)
      else
        setColor(CARET_COLOR, settings.jumpModeColor)
      Canvas.repaint()
    }

  private fun Editor.restoreSettings() {
    restoreScroll()
    restoreCanvas()
    restoreCursor()
    restoreColors()
  }

  private fun Editor.restoreCanvas() =
    component.run {
      Canvas.reset()
      remove(Canvas)
      repaint()
    }

  private fun Editor.restoreCursor() = runNow {
    settings.isBlinkCaret = Model.naturalBlink
    settings.isBlockCursor = Model.naturalBlock
  }

  private fun Editor.restoreColors() = runNow {
    colorsScheme.run {
      setColor(CARET_COLOR, Model.naturalColor)
      setAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES,
        getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES)
          .apply { backgroundColor = Model.naturalHighlight })
    }
  }
}