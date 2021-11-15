package org.acejump.session

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import com.intellij.util.containers.ContainerUtil
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.acejump.*
import org.acejump.action.TagScroller
import org.acejump.action.TagJumper
import org.acejump.action.TagVisitor
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries.VISIBLE_ON_SCREEN
import org.acejump.boundaries.StandardBoundaries.WHOLE_FILE
import org.acejump.config.AceConfig
import org.acejump.input.EditorKeyListener
import org.acejump.input.JumpMode
import org.acejump.input.JumpModeTracker
import org.acejump.input.KeyLayoutCache
import org.acejump.search.Pattern
import org.acejump.search.SearchProcessor
import org.acejump.search.Tagger
import org.acejump.search.TaggingResult
import org.acejump.view.TagCanvas
import org.acejump.view.TextHighlighter
import java.util.*

/**
 * Manages an AceJump session for one or more [Editor]s.
 */
class Session(private val mainEditor: Editor, private val jumpEditors: List<Editor>) {
  private val listeners: MutableList<AceJumpListener> =
    ContainerUtil.createLockFreeCopyOnWriteList()

  private var boundaries: Boundaries = defaultBoundaries

  private companion object {
    private val defaultBoundaries
      get() = if (AceConfig.searchWholeFile) WHOLE_FILE else VISIBLE_ON_SCREEN
  }

  private val originalSettings = EditorSettings.setup(mainEditor)

  private val jumpModeTracker = JumpModeTracker()
  private var jumpMode = JumpMode.DISABLED
    set(value) {
      field = value

      if (value === JumpMode.DISABLED) {
        end()
      } else {
        searchProcessor?.let { textHighlighter.render(it.results, it.query, jumpMode) }
        mainEditor.colorsScheme.setColor(CARET_COLOR, value.caretColor)
        mainEditor.contentComponent.repaint()
      }
    }

  private var searchProcessor: SearchProcessor? = null
  private var tagger = Tagger(jumpEditors)

  private val tagJumper
    get() = TagJumper(jumpMode, searchProcessor)

  private val tagVisitor
    get() = searchProcessor?.let { TagVisitor(mainEditor, it, tagJumper) }

  private val tagScroller
    get() = searchProcessor?.let { TagScroller(mainEditor, it) }

  private val textHighlighter = TextHighlighter()
  private val tagCanvases = jumpEditors.associateWith(::TagCanvas)

  @ExternalUsage
  val tags
    get() = tagger.tags

  init {
    KeyLayoutCache.ensureInitialized(AceConfig.settings)

    EditorKeyListener.attach(mainEditor, object: TypedActionHandler {
      override fun execute(editor: Editor, charTyped: Char, context: DataContext) {
        var processor = searchProcessor
        val hadTags = tagger.hasTags

        if (processor == null) {
          processor = SearchProcessor.fromChar(
            jumpEditors, charTyped, boundaries
          ).also { searchProcessor = it }
        } else if (!processor.type(charTyped, tagger)) {
          return
        }

        updateSearch(
          processor, markImmediately = hadTags,
          shiftMode = charTyped.isUpperCase()
        )
      }
    })
  }

  /**
   * Updates text highlights and tag markers according to the current
   * search state. Dispatches jumps if the search query matches a tag.
   * If all tags are outside view, scrolls to the closest one.
   */
  private fun updateSearch(
    processor: SearchProcessor,
    markImmediately: Boolean,
    shiftMode: Boolean = false
  ) {
    val query = processor.query
    val results = processor.results

    textHighlighter.render(results, query, jumpMode)

    if (!markImmediately &&
      query.rawText.let {
        it.length < AceConfig.minQueryLength &&
          it.all(Char::isLetterOrDigit)
      }
    ) {
      return
    }

    when (val result = tagger.markOrJump(query, results.clone())) {
      is TaggingResult.Jump -> {
        tagJumper.jump(result.tag, shiftMode, isCrossEditor = mainEditor !== result.tag.editor)
        tagCanvases.values.forEach(TagCanvas::removeMarkers)
        end(result)
      }

      is TaggingResult.Mark -> {
        val markers = result.markers
        
        for ((editor, canvas) in tagCanvases) {
          canvas.setMarkers(markers[editor].orEmpty())
        }
        
        if (jumpEditors.all { editor ->
            val cache = EditorOffsetCache.new()
            markers[editor].let { it == null || it.none { marker ->
              VISIBLE_ON_SCREEN.isOffsetInside(editor, marker.offsetL, cache) ||
              VISIBLE_ON_SCREEN.isOffsetInside(editor, marker.offsetR, cache) } }
        }) {
          tagVisitor?.scrollToClosest()
        }
      }
    }
  }

  @ExternalUsage
  fun markResults(resultsToMark: SortedSet<Int>) {
    val jumpEditor = jumpEditors.singleOrNull() ?: return
    markResults(mapOf(jumpEditor to resultsToMark))
  }
  
  @ExternalUsage
  fun markResults(resultsToMark: Map<Editor, Collection<Int>>) {
    tagger = Tagger(jumpEditors)
    tagCanvases.values.forEach { it.setMarkers(emptyList()) }
  
    val processor = SearchProcessor.fromRegex(jumpEditors, "", defaultBoundaries)
      .apply {
        results.clear()
        for ((editor, offsets) in resultsToMark) {
          if (editor in jumpEditors) {
            results[editor] = IntArrayList(offsets)
          }
        }
      }
  
    updateSearch(processor, markImmediately = true)
  }

  /**
   * Starts a regular expression search. If a search was already active,
   * it will be reset alongside its tags and highlights.
   */

  @ExternalUsage
  fun startRegexSearch(pattern: String, boundaries: Boundaries) {
    tagger = Tagger(jumpEditors)
    tagCanvases.values.forEach { it.setMarkers(emptyList()) }

    val processor = SearchProcessor.fromRegex(
      jumpEditors, pattern,
      boundaries.intersection(defaultBoundaries)
    ).also { searchProcessor = it }
    updateSearch(processor, markImmediately = true)
  }

  /**
   * Starts a regular expression search. If a search was already active,
   * it will be reset alongside its tags and highlights.
   */

  @ExternalUsage
  fun startRegexSearch(pattern: Pattern, boundaries: Boundaries) =
    startRegexSearch(pattern.regex, boundaries)

  /**
   * See [JumpModeTracker.cycle].
   */
  fun cycleNextJumpMode() {
    jumpMode = jumpModeTracker.cycle(forward = true)
  }

  /**
   * See [JumpModeTracker.cycle].
   */
  fun cyclePreviousJumpMode() {
    jumpMode = jumpModeTracker.cycle(forward = false)
  }

  /**
   * See [JumpModeTracker.toggle]
   */
  fun toggleJumpMode(newMode: JumpMode) {
    jumpMode = jumpModeTracker.toggle(newMode)
  }

  @ExternalUsage
  fun toggleJumpMode(newMode: JumpMode, boundaries: Boundaries) {
    this.boundaries = this.boundaries.intersection(boundaries)
    jumpMode = jumpModeTracker.toggle(newMode)
  }

  /**
   * See [TagVisitor.visitPrevious]. If there are no tags, nothing happens.
   */
  fun visitPreviousTag() =
    if (tagVisitor?.visitPrevious() == true) end() else Unit

  /**
   * See [TagVisitor.visitNext]. If there are no tags, nothing happens.
   */
  fun visitNextTag() =
    if (tagVisitor?.visitNext() == true) end() else Unit

  /**
   * See [TagVisitor.visitPrevious]. If there are no tags, nothing happens.
   */
  fun scrollToNextScreenful() = tagScroller?.scroll(true)

  /**
   * See [TagVisitor.visitNext]. If there are no tags, nothing happens.
   */
  fun scrollToPreviousScreenful() = tagScroller?.scroll(false)

  /**
   * Ends this session.
   */
  fun end(taggingResult: TaggingResult? = null) =
    SessionManager.end(mainEditor, taggingResult)

  /**
   * Clears any currently active search, tags, and highlights.
   * Does not reset [JumpMode].
   */
  fun restart() {
    tagger = Tagger(jumpEditors)
    searchProcessor = null
    tagCanvases.values.forEach(TagCanvas::removeMarkers)
    textHighlighter.reset()
  }

  /**
   * Should only be used from [SessionManager] to dispose a
   * successfully ended session.
   */
  internal fun dispose(taggingResult: TaggingResult?) {
    tagger = Tagger(jumpEditors)
    EditorKeyListener.detach(mainEditor)
    tagCanvases.values.forEach(TagCanvas::unbind)
    textHighlighter.reset()
    EditorsCache.invalidate()

    val jumpResult = taggingResult as? TaggingResult.Jump
    val mark = jumpResult?.mark
    val query = jumpResult?.query
    listeners.forEach { it.finished(mark, query) }

    if (!mainEditor.isDisposed) {
      originalSettings.restore(mainEditor)
      mainEditor.colorsScheme.setColor(CARET_COLOR, JumpMode.DISABLED.caretColor)
    }

    val focusedEditor = jumpResult?.tag?.editor ?: mainEditor
    if (!focusedEditor.isDisposed) {
      focusedEditor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
  }

  @ExternalUsage
  fun addAceJumpListener(listener: AceJumpListener) {
    listeners += listener
  }

  @ExternalUsage
  fun removeAceJumpListener(listener: AceJumpListener) {
    listeners -= listener
  }
}
