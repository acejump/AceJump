package org.acejump.session

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.colors.EditorColors
import org.acejump.ExternalUsage
import org.acejump.action.TagJumper
import org.acejump.action.TagVisitor
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries
import org.acejump.config.AceConfig
import org.acejump.input.EditorKeyListener
import org.acejump.input.JumpMode
import org.acejump.input.JumpModeTracker
import org.acejump.input.KeyLayoutCache
import org.acejump.search.*
import org.acejump.view.TagCanvas
import org.acejump.view.TextHighlighter

/**
 * Manages an AceJump session for a single [Editor].
 */
class Session(private val editor: Editor) {
  private companion object {
    private val defaultBoundaries
      get() = if (AceConfig.searchWholeFile) StandardBoundaries.WHOLE_FILE else StandardBoundaries.VISIBLE_ON_SCREEN
  }
  
  private val originalSettings = EditorSettings.setup(editor)
  
  private val jumpModeTracker = JumpModeTracker()
  private var jumpMode = JumpMode.DISABLED
    set(value) {
      field = value
      
      if (value === JumpMode.DISABLED) {
        end()
      }
      else {
        searchProcessor?.let { textHighlighter.render(it.results, it.query, jumpMode) }
        editor.colorsScheme.setColor(EditorColors.CARET_COLOR, value.caretColor)
        editor.contentComponent.repaint()
      }
    }
  
  private var searchProcessor: SearchProcessor? = null
  private var tagger = Tagger(editor)
  
  private val tagJumper
    get() = TagJumper(editor, jumpMode, searchProcessor)
  
  private val tagVisitor
    get() = searchProcessor?.let { TagVisitor(editor, it, tagJumper) }
  
  private val textHighlighter = TextHighlighter(editor)
  private val tagCanvas = TagCanvas(editor)
  
  @ExternalUsage
  val tags
    get() = tagger.tags
  
  init {
    KeyLayoutCache.ensureInitialized(AceConfig.settings)
    
    EditorKeyListener.attach(editor, object : TypedActionHandler {
      override fun execute(editor: Editor, charTyped: Char, context: DataContext) {
        var processor = searchProcessor
        
        if (processor == null) {
          processor = SearchProcessor.fromChar(editor, charTyped, defaultBoundaries).also { searchProcessor = it }
        }
        else if (!processor.type(charTyped, tagger)) {
          return
        }
        
        updateSearch(processor, shiftMode = charTyped.isUpperCase())
      }
    })
  }
  
  /**
   * Updates text highlights and tag markers according to the current search state. Dispatches jumps if the search query matches a tag.
   * If all tags are outside view, scrolls to the closest one.
   */
  private fun updateSearch(processor: SearchProcessor, shiftMode: Boolean = false) {
    val query = processor.query
    val results = processor.results
    
    textHighlighter.render(results, query, jumpMode)
    
    when (val result = tagger.markOrJump(query, results.clone())) {
      is TaggingResult.Jump -> {
        tagJumper.jump(result.offset, shiftMode)
        tagCanvas.removeMarkers()
        end()
      }
      
      is TaggingResult.Mark -> {
        val tags = result.tags
        tagCanvas.setMarkers(tags, isRegex = query is SearchQuery.RegularExpression)
        
        val cache = EditorOffsetCache.new()
        val boundaries = StandardBoundaries.VISIBLE_ON_SCREEN
        
        if (tags.none { boundaries.isOffsetInside(editor, it.offsetL, cache) || boundaries.isOffsetInside(editor, it.offsetR, cache) }) {
          tagVisitor?.scrollToClosest()
        }
      }
    }
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: String, boundaries: Boundaries) {
    tagger = Tagger(editor)
    tagCanvas.setMarkers(emptyList(), isRegex = true)
    updateSearch(SearchProcessor.fromRegex(editor, pattern, boundaries.intersection(defaultBoundaries)).also { searchProcessor = it })
  }
  
  /**
   * Starts a regular expression search. If a search was already active, it will be reset alongside its tags and highlights.
   */
  fun startRegexSearch(pattern: Pattern, boundaries: Boundaries) {
    startRegexSearch(pattern.regex, boundaries)
  }
  
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
  
  /**
   * See [TagVisitor.visitPrevious]. If there are no tags, nothing happens.
   */
  fun visitPreviousTag() {
    if (tagVisitor?.visitPrevious() == true) {
      end()
    }
  }
  
  /**
   * See [TagVisitor.visitNext]. If there are no tags, nothing happens.
   */
  fun visitNextTag() {
    if (tagVisitor?.visitNext() == true) {
      end()
    }
  }
  
  /**
   * Ends this session.
   */
  fun end() {
    SessionManager.end(editor)
  }
  
  /**
   * Clears any currently active search, tags, and highlights. Does not reset [JumpMode].
   */
  fun restart() {
    tagger = Tagger(editor)
    searchProcessor = null
    tagCanvas.removeMarkers()
    textHighlighter.reset()
  }
  
  /**
   * Should only be used from [SessionManager] to dispose a successfully ended session.
   */
  internal fun dispose() {
    tagger = Tagger(editor)
    EditorKeyListener.detach(editor)
    tagCanvas.unbind()
    textHighlighter.reset()
    
    if (!editor.isDisposed) {
      originalSettings.restore(editor)
      editor.colorsScheme.setColor(EditorColors.CARET_COLOR, JumpMode.DISABLED.caretColor)
      editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    }
  }
}
