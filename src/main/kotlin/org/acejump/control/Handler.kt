package org.acejump.control

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.*
import com.intellij.openapi.editor.colors.EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
import org.acejump.control.Scroller.restoreScroll
import org.acejump.control.Scroller.saveScroll
import org.acejump.label.Pattern
import org.acejump.label.Pattern.*
import org.acejump.label.Tagger
import org.acejump.search.*
import org.acejump.search.Finder.search
import org.acejump.view.*
import org.acejump.view.Boundary.FULL_FILE_BOUNDARY
import org.acejump.view.Canvas.bindCanvas
import org.acejump.view.Model.editor
import org.acejump.view.Model.setupCaret

/**
 * Handles all incoming keystrokes, IDE notifications, and UI updates.
 */

object Handler: TypedActionHandler, Resettable {
  private val logger = Logger.getInstance(Handler::class.java)
  private var enabled = false
  private val typingAction = EditorActionManager.getInstance().typedAction
  private val oldHandler = typingAction.rawHandler

  private val editorActionMap = mutableMapOf<String, EditorActionHandler>(
    ACTION_EDITOR_BACKSPACE to makeHandler { clear() },
    ACTION_EDITOR_START_NEW_LINE to makeHandler{ Selector.select(false) },
    ACTION_EDITOR_ENTER to makeHandler { Selector.select() },
    ACTION_EDITOR_TAB to makeHandler { Scroller.scroll(true) },
    ACTION_EDITOR_PREV_PARAMETER to makeHandler { Scroller.scroll(false) },
    ACTION_EDITOR_MOVE_CARET_UP to makeHandler { regexSearch(CODE_INDENTS) },
    ACTION_EDITOR_MOVE_CARET_LEFT to makeHandler { regexSearch(START_OF_LINE) },
    ACTION_EDITOR_MOVE_CARET_RIGHT to makeHandler { regexSearch(END_OF_LINE) },
    ACTION_EDITOR_MOVE_LINE_START to makeHandler { regexSearch(START_OF_LINE) },
    ACTION_EDITOR_MOVE_LINE_END to makeHandler { regexSearch(END_OF_LINE) },
    ACTION_EDITOR_ESCAPE to makeHandler { reset() }
  )

  private fun makeHandler(handle: () -> Unit) = object: EditorActionHandler() {
    override fun doExecute(e: Editor, c: Caret?, dc: DataContext?) = handle()
  }

  fun regexSearch(regex: Pattern, bounds: Boundary = FULL_FILE_BOUNDARY) =
    Canvas.reset().also { search(regex, bounds) }

  fun activate() = runAndWait { if (!enabled) configureEditor() }

  private fun clear() {
    applyTo(Tagger, Jumper, Finder, Canvas) { reset() }
    repaintTagMarkers()
  }

  private fun installKeyHandler() = typingAction.setupRawHandler(this)
  private fun uninstallKeyHandler() = typingAction.setupRawHandler(oldHandler)

  override fun execute(editor: Editor, key: Char, dataContext: DataContext) {
    logger.info("Intercepted keystroke: $key")
    Finder.query += key // This will trigger an update
  }

  private fun configureEditor() =
    editor.run {
      enabled = true
      swapActions()
      saveScroll()
      setupCaret()
      bindCanvas()
      installKeyHandler()
      Listener.enable()
    }

  private fun swapActions() = EditorActionManager.getInstance().run {
    editorActionMap.forEach { actionId, handler ->
      editorActionMap[actionId] = getActionHandler(actionId)
      setActionHandler(actionId, handler)
    }
  }

  fun repaintTagMarkers() {
    if (Canvas.jumpLocations.isEmpty() || Tagger.markers.size <= Canvas.jumpLocations.size) {
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

    clear()
    editor.restoreSettings()
  }

  private fun Editor.restoreSettings() = runAndWait {
    enabled = false
    swapActions()
    uninstallKeyHandler()
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
      setAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES,
        getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES)
          .apply { backgroundColor = Model.naturalHighlight })
    }
  }
}