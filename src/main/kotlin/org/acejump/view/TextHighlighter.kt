package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.util.ui.JBUI
import it.unimi.dsi.fastutil.ints.IntList
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.config.AceConfig
import org.acejump.immutableText
import org.acejump.input.JumpMode
import org.acejump.isWordPart
import org.acejump.search.SearchQuery
import org.acejump.wordEnd
import org.acejump.wordStart
import java.awt.Graphics
import kotlin.math.max

/**
 * Renders highlights for search occurrences.
 */
internal class TextHighlighter(private val editor: Editor) {
  private companion object {
    private const val LAYER = HighlighterLayer.LAST + 1
  }
  
  private var previousHighlights: Array<RangeHighlighter>? = null
  
  /**
   * Removes all current highlights and re-creates them from scratch. Must be called whenever any of the method parameters change.
   */
  fun render(offsets: IntList, query: SearchQuery, jumpMode: JumpMode) {
    val markup = editor.markupModel
    val chars = editor.immutableText
    
    val renderer = when {
      query is SearchQuery.RegularExpression -> RegexRenderer
      jumpMode === JumpMode.TARGET           -> SearchedWordWithOutlineRenderer
      else                                   -> SearchedWordRenderer
    }
    
    val modifications = (previousHighlights?.size ?: 0) + offsets.size
    val enableBulkEditing = modifications > 1000
    
    val document = editor.document
    
    try {
      if (enableBulkEditing) {
        document.isInBulkUpdate = true
      }
      
      previousHighlights?.forEach(markup::removeHighlighter)
      previousHighlights = Array(offsets.size) { index ->
        val start = offsets.getInt(index)
        val end = start + query.getHighlightLength(chars, start)
        
        markup.addRangeHighlighter(start, end, LAYER, null, HighlighterTargetArea.EXACT_RANGE).apply {
          customRenderer = renderer
        }
      }
    } finally {
      if (enableBulkEditing) {
        document.isInBulkUpdate = false
      }
    }
  }
  
  fun reset() {
    editor.markupModel.removeAllHighlighters()
    previousHighlights = null
  }
  
  /**
   * Renders a filled highlight in the background of a searched text occurrence.
   */
  private object SearchedWordRenderer : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      drawFilled(g, editor, highlighter.startOffset, highlighter.endOffset)
    }
    
    private fun drawFilled(g: Graphics, editor: Editor, startOffset: Int, endOffset: Int) {
      val start = EditorOffsetCache.Uncached.offsetToXY(editor, startOffset)
      val end = EditorOffsetCache.Uncached.offsetToXY(editor, endOffset)
      
      g.color = AceConfig.textHighlightColor
      g.fillRect(start.x, start.y + 1, end.x - start.x, editor.lineHeight - 1)
  
      g.color = AceConfig.tagBackgroundColor
      g.drawRect(start.x, start.y, end.x - start.x, editor.lineHeight)
    }
  }
  
  /**
   * Renders a filled highlight in the background of a searched text occurrence, as well as an outline indicating the range of characters
   * that will be selected by [JumpMode.TARGET].
   */
  private object SearchedWordWithOutlineRenderer : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      SearchedWordRenderer.paint(editor, highlighter, g)
      
      val chars = editor.immutableText
      val startOffset = highlighter.startOffset
      
      if (chars.getOrNull(startOffset)?.isWordPart == true) {
        drawOutline(g, editor, chars.wordStart(startOffset), chars.wordEnd(startOffset) + 1)
      }
    }
    
    private fun drawOutline(g: Graphics, editor: Editor, startOffset: Int, endOffset: Int) {
      val start = EditorOffsetCache.Uncached.offsetToXY(editor, startOffset)
      val end = EditorOffsetCache.Uncached.offsetToXY(editor, endOffset)
      
      g.color = AceConfig.targetModeColor
      g.drawRect(max(0, start.x - JBUI.scale(1)), start.y, end.x - start.x + JBUI.scale(2), editor.lineHeight)
    }
  }
  
  /**
   * Renders a filled highlight in the background of the first highlighted position. Used for regex search queries.
   */
  private object RegexRenderer : CustomHighlighterRenderer {
    override fun paint(editor: Editor, highlighter: RangeHighlighter, g: Graphics) {
      drawSingle(g, editor, highlighter.startOffset)
    }
    
    private fun drawSingle(g: Graphics, editor: Editor, offset: Int) {
      val pos = EditorOffsetCache.Uncached.offsetToXY(editor, offset)
      val char = editor.immutableText.getOrNull(offset)?.takeUnless { it == '\n' || it == '\t' } ?: ' '
      val font = editor.colorsScheme.getFont(EditorFontType.PLAIN)
      val lastCharWidth = editor.component.getFontMetrics(font).charWidth(char)
      
      g.color = AceConfig.textHighlightColor
      g.fillRect(pos.x, pos.y + 1, lastCharWidth, editor.lineHeight - 1)
      
      g.color = AceConfig.tagBackgroundColor
      g.drawRect(pos.x, pos.y, lastCharWidth, editor.lineHeight)
    }
  }
}
