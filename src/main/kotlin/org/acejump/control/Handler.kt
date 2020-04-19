package org.acejump.control

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.actionSystem.TypedAction
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.openapi.editor.colors.EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.util.containers.ContainerUtil
import org.acejump.control.Scroller.restoreScroll
import org.acejump.control.Scroller.saveScroll
import org.acejump.label.Pattern
import org.acejump.label.Pattern.*
import org.acejump.label.Tagger
import org.acejump.search.*
import org.acejump.view.Boundary
import org.acejump.view.Boundary.FULL_FILE_BOUNDARY
import org.acejump.view.Canvas
import org.acejump.view.Canvas.bindCanvas
import org.acejump.view.Model
import org.acejump.view.Model.editor
import org.acejump.view.Model.setupCaret
import java.awt.Color

/**
 * Handles all incoming keystrokes, IDE notifications, and UI updates.
 */

object Handler : TypedActionHandler, Resettable {
  private val listeners: MutableList<AceJumpListener> = ContainerUtil.createLockFreeCopyOnWriteList()
  private val logger = Logger.getInstance(Handler::class.java)
  private var enabled = false
  private val typingAction = TypedAction.getInstance()
  private val oldHandler = typingAction.rawHandler

  private val editorActionMap = mutableMapOf<String, EditorActionHandler>(
    ACTION_EDITOR_BACKSPACE to makeHandler { clear() },
    ACTION_EDITOR_START_NEW_LINE to makeHandler { Selector.select(false) },
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

  private fun makeHandler(handle: () -> Unit) = object : EditorActionHandler() {
    override fun doExecute(e: Editor, c: Caret?, dc: DataContext?) = handle()
  }

  @ExternalUsage
  fun regexSearch(regex: Pattern, bounds: Boundary = FULL_FILE_BOUNDARY) =
    Canvas.reset().also { Finder.search(regex, bounds) }

  @ExternalUsage
  fun customRegexSearch(regex: String, bounds: Boundary = FULL_FILE_BOUNDARY) =
    Canvas.reset().also { Finder.search(regex, bounds) }

  fun activate() = if (!enabled) configureEditor() else { }

  private fun clear() {
    applyTo(Tagger, Jumper, Finder, Canvas) { reset() }
    repaintTagMarkers()
  }

  private fun installKeyHandler() = typingAction.setupRawHandler(this)
  private fun uninstallKeyHandler() = typingAction.setupRawHandler(oldHandler)

  /**
   * TODO: Integrate query highlighting with [AceSearchSession]
   */

  var session: AceSearchSession? = null
  override fun execute(editor: Editor, key: Char, dataContext: DataContext) {
    logger.info("Intercepted keystroke: $key")
    Finder.query += key // This will trigger an update

//    if (session == null)
//      session = AceSearchSession(editor, AceFindModel(key.toString()))
//    else session?.findModel!!.stringToFind += key
  }

  private fun configureEditor() =
    editor.run {
      enabled = true
      saveScroll()
      saveCaret()
      saveColors()
      runNow {
        setupCaret()
        bindCanvas()
        swapActionHandler()
        installKeyHandler()
        Listener.enable()
      }
    }

  private fun swapActionHandler() = EditorActionManager.getInstance().run {
    editorActionMap.forEach { (actionId, handler) ->
      editorActionMap[actionId] = getActionHandler(actionId)
      setActionHandler(actionId, handler)
    }
  }

  fun repaintTagMarkers() {
    if (Tagger.tagSelected) reset() else Canvas.jumpLocations = Tagger.markers
  }

  fun redoFind() {
    runNow {
      editor.run {
        restoreCanvas()
        bindCanvas()
      }
    }

    if (Finder.query.isNotEmpty() || Tagger.regex) {
      Finder.search()
      repaintTagMarkers()
    }
  }

  override fun reset() {
    if (enabled) Listener.disable()
    session = null

    // In order to get Finder.query value, listeners should
    //  be placed before cleanup
    listeners.forEach(AceJumpListener::finished)
    clear()
    editor.restoreSettings()
  }

  @ExternalUsage
  fun addAceJumpListener(listener: AceJumpListener) {
    listeners += listener
  }

  @ExternalUsage
  fun removeAceJumpListener(listener: AceJumpListener) {
    listeners -= listener
  }

  private fun Editor.restoreSettings() = runNow {
    enabled = false
    swapActionHandler()
    uninstallKeyHandler()
    if(!isDisposed) {
      restoreScroll()
      restoreCanvas()
      restoreCaret()
      restoreColors()
    }
  }

  private fun Editor.restoreCanvas() =
    contentComponent.run {
      remove(Canvas)
      repaint()
    }

  private fun saveCaret() {
    editor.settings.run {
      Model.naturalBlock = isBlockCursor
      Model.naturalBlink = isBlinkCaret
    }

    editor.caretModel.primaryCaret.visualAttributes.run {
      Model.naturalCaretColor = color
        ?: EditorColorsManager.getInstance().globalScheme.getColor(CARET_COLOR)
          ?: Color.BLACK
    }
  }

  private fun saveColors() =
    EditorColorsManager.getInstance().globalScheme
      .getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES)
      ?.backgroundColor.let { Model.naturalHighlight = it }

  private fun Editor.restoreCaret() =
      runNow {
        settings.isBlinkCaret = Model.naturalBlink
        settings.isBlockCursor = Model.naturalBlock
      }

  private fun Editor.restoreColors() =
    runNow {
      colorsScheme.run {
        setAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES,
          getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES)
            .apply { backgroundColor = Model.naturalHighlight })
      }
    }

  interface AceJumpListener {
    fun finished() {}
  }
}