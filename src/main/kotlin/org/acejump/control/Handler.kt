package org.acejump.control

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
import com.intellij.util.SmartList
import org.acejump.config.AceConfig.Companion.settings
import org.acejump.label.Pattern
import org.acejump.label.Pattern.*
import org.acejump.label.Tagger
import org.acejump.search.*
import org.acejump.search.Finder.search
import org.acejump.search.Scroller.restoreScroll
import org.acejump.search.Scroller.storeScroll
import org.acejump.view.Boundary
import org.acejump.view.Boundary.FullFileBoundary
import org.acejump.view.Canvas
import org.acejump.view.Canvas.bindCanvas
import org.acejump.view.Model
import org.acejump.view.Model.editor
import org.acejump.view.Model.setupCaret
import java.awt.event.KeyEvent.*
import javax.swing.JComponent

/**
 * Handles all incoming keystrokes, IDE notifications, and UI updates.
 */

object Handler : TypedActionHandler, Resettable {
  private val logger = Logger.getInstance(Handler::class.java)
  private var enabled = false
  private val editorTypeAction = EditorActionManager.getInstance().typedAction
  private val handler = editorTypeAction.rawHandler
  private var isShiftDown = false
  private val keyMap = mapOf(
    VK_HOME to { regexSearch(START_OF_LINE) },
    VK_LEFT to { regexSearch(START_OF_LINE) },
    VK_RIGHT to { regexSearch(END_OF_LINE) },
    VK_END to { regexSearch(END_OF_LINE) },
    VK_UP to { regexSearch(CODE_INDENTS) },
    VK_ESCAPE to { reset() },
    VK_BACK_SPACE to { clear() },
    VK_ENTER to { Tagger.jumpToNextOrNearestVisible(); repaintTagMarkers() },
    // TODO: recycle tags during tab search, push scanner as far as possible
    VK_TAB to { Scroller.ifQueryExistsScrollToNextOccurrence(!isShiftDown) },
    VK_SPACE to { processSpacebar() }
  )

  fun regexSearch(regex: Pattern, bounds: Boundary = FullFileBoundary) = Canvas.reset().also { search(regex, bounds) }

  fun activate() = runAndWait { if (!enabled) start() else toggleTargetMode() }

  fun processCommand(keyCode: Int) = keyMap[keyCode]?.invoke()

  private fun processSpacebar() =
    if (Finder.query.isEmpty()) regexSearch(ALL_WORDS) else Finder.query += " "

  private fun clear() {
    applyTo(Tagger, Jumper, Finder, Canvas) { reset() }
    repaintTagMarkers()
  }

  private fun installSearchKeyHandler() = editorTypeAction.setupRawHandler(this)

  override fun execute(editor: Editor, key: Char, dataContext: DataContext) {
    logger.info("Intercepted keystroke: $key")
    Finder.query += key // This will trigger an update
  }

  private fun configureEditor() =
    editor.run {
      storeScroll()
      setupCaret()
      bindCanvas()
      installSearchKeyHandler()
      Listener.enable()
      component.installCustomShortcutHandler()
    }

  private var defaultAction: List<*>? = null

  // TODO: Replace this with `BuildInfo.api`, check: idea/146.1211+ ref.:
  // https://github.com/JetBrains/intellij-community/commit/10f16fcd919e4f846930eaa65943e373267f2039#diff-83ee176e2a4882290ced6a65443866cbL70
  private val ACTIONS_KEY = AnAction::class.java.declaredFields.first {
    it.name == "ACTIONS_KEY" || it.name == "ourClientProperty"
  }.get(null)

  // Investigate replacing this with `IDEEventQueue.*Dispatcher(...)`
  private fun JComponent.installCustomShortcutHandler() {
    logger.info("Installing custom shortcuts")
    defaultAction = getClientProperty(ACTIONS_KEY) as List<*>?
    putClientProperty(ACTIONS_KEY, SmartList<AnAction>(AceKeyAction))
    val css = CustomShortcutSet(*keyMap.keys.toTypedArray())
    AceKeyAction.registerCustomShortcutSet(css, this)
  }

  private fun JComponent.uninstallCustomShortCutHandler() {
    logger.info("Uninstalling custom shortcuts")
    putClientProperty(ACTIONS_KEY, defaultAction)
    AceKeyAction.unregisterCustomShortcutSet(this)
    editorTypeAction.setupRawHandler(handler)
  }

  private fun start() {
    enabled = true
    configureEditor()
  }

  fun repaintTagMarkers() {
    if(Canvas.jumpLocations.isEmpty() || Tagger.markers.size <= Canvas.jumpLocations.size) {
      if (Jumper.hasJumped) reset() else Canvas.jumpLocations = Tagger.markers
    }
  }

  fun redoFind() {
    runAndWait {
      editor.run {
        restoreCanvas()
        bindCanvas()
      }
    }

    if (Finder.query.isNotEmpty() || Tagger.regex)
      runLater {
        Finder.search()
        repaintTagMarkers()
      }
  }

  override fun reset() {
    if (enabled) Listener.disable()
    editor.component.uninstallCustomShortCutHandler()
    enabled = false
    clear()
    editor.restoreSettings()
  }

  fun toggleTargetMode(status: Boolean? = null) =
    editor.colorsScheme.run {
      if (Jumper.toggleTargetMode(status))
        setColor(CARET_COLOR, settings.targetModeColor)
      else
        setColor(CARET_COLOR, settings.jumpModeColor)

      Finder.paintTextHighlights()
      Canvas.repaint()
    }

  fun toggleDefinitionMode(status: Boolean? = null) =
    editor.colorsScheme.run {
      if (Jumper.toggleDefinitionMode(status))
        setColor(CARET_COLOR, settings.definitionModeColor)
      else
        setColor(CARET_COLOR, settings.jumpModeColor)

      Finder.paintTextHighlights()
      Canvas.repaint()
    }

  private fun Editor.restoreSettings() = runAndWait {
    restoreScroll()
    restoreCanvas()
    restoreCaret()
    restoreColors()
  }

  private fun Editor.restoreCanvas() =
    contentComponent.run {
      remove(Canvas)
      repaint()
    }

  private fun Editor.restoreCaret() = runAndWait {
    settings.isBlinkCaret = Model.naturalBlink
    settings.isBlockCursor = Model.naturalBlock
  }

  private fun Editor.restoreColors() = runAndWait {
    colorsScheme.run {
      setColor(CARET_COLOR, Model.naturalColor)
      setAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES,
        getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES)
          .apply { backgroundColor = Model.naturalHighlight })
    }
  }
}