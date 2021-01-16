package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.ui.ColorUtil
import com.intellij.ui.scale.JBUIScale
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries
import org.acejump.config.AceConfig
import org.acejump.countMatchingCharacters
import org.acejump.immutableText
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.max

/**
 * Describes a 1 or 2 character shortcut that points to a specific character in the editor.
 */
internal class Tag(
  private val tag: String,
  val offsetL: Int,
  val offsetR: Int,
  private val shiftR: Int,
  private val hasSpaceRight: Boolean
) {
  private val length = tag.length
  
  companion object {
    const val ARC = 1
    
    /**
     * Creates a new tag, precomputing some information about the nearby characters to reduce rendering overhead. If the last typed
     * character ([literalQueryText]) matches the first [tag] character, only the second [tag] character is displayed.
     */
    fun create(editor: Editor, tag: String, offset: Int, literalQueryText: String?): Tag {
      val chars = editor.immutableText
      val matching = literalQueryText?.let { chars.countMatchingCharacters(offset, it) } ?: 0
      val hasSpaceRight = offset + 1 >= chars.length || chars[offset + 1].isWhitespace()
      
      val displayedTag = if (literalQueryText != null && literalQueryText.last().equals(tag.first(), ignoreCase = true))
        tag.drop(1).toUpperCase()
      else
        tag.toUpperCase()
      
      return Tag(displayedTag, offset, offset + max(0, matching - 1), tag.length - displayedTag.length, hasSpaceRight)
    }
    
    /**
     * Renders the tag background.
     */
    private fun drawHighlight(g: Graphics2D, rect: Rectangle, color: Color) {
      g.color = color
      g.fillRoundRect(rect.x, rect.y + 1, rect.width, rect.height - 1, ARC, ARC)
    }
    
    /**
     * Renders the tag text.
     */
    private fun drawForeground(g: Graphics2D, font: TagFont, point: Point, text: String) {
      g.font = font.tagFont
      g.color = AceConfig.tagForegroundColor
      g.drawString(text, point.x, point.y + font.baselineDistance)
    }
  }
  
  /**
   * Returns true if the left-aligned offset is in the range. Use to cull tags outside visible range.
   * Only the left offset is checked, because if the tag was right-aligned on the last index of the range, it would not be visible anyway.
   */
  fun isOffsetInRange(range: IntRange): Boolean {
    return offsetL in range
  }
  
  /**
   * Determines on which side of the target character the tag is positioned.
   */
  enum class TagAlignment {
    LEFT,
    RIGHT
  }
  
  /**
   * Paints the tag, taking into consideration visual space around characters in the editor, as well as all other previously painted tags.
   * Returns a rectangle indicating the area where the tag was rendered, or null if the tag could not be rendered due to overlap.
   */
  fun paint(
    g: Graphics2D, editor: Editor, cache: EditorOffsetCache, font: TagFont, occupied: MutableList<Rectangle>, isRegex: Boolean
  ): Rectangle? {
    val (rect, alignment) = alignTag(editor, cache, font, occupied) ?: return null
    
    val highlightColor = when {
      alignment != TagAlignment.RIGHT || hasSpaceRight || isRegex -> AceConfig.tagBackgroundColor
      else                                                        -> ColorUtil.darker(AceConfig.tagBackgroundColor, 3)
    }
    
    drawHighlight(g, rect, highlightColor)
    drawForeground(g, font, rect.location, tag)
    
    occupied.add(JBUIScale.scale(2).let { Rectangle(rect.x - it, rect.y, rect.width + (2 * it), rect.height) })
    return rect
  }
  
  private fun alignTag(editor: Editor, cache: EditorOffsetCache, font: TagFont, occupied: List<Rectangle>): Pair<Rectangle, TagAlignment>? {
    val boundaries = StandardBoundaries.VISIBLE_ON_SCREEN
    
    if (hasSpaceRight || offsetL == 0 || editor.immutableText[offsetL - 1].let { it == '\n' || it == '\r' }) {
      val rectR = createRightAlignedTagRect(editor, cache, font)
      
      return (rectR to TagAlignment.RIGHT).takeIf {
        boundaries.isOffsetInside(editor, offsetR, cache) && occupied.none(rectR::intersects)
      }
    }
    
    val rectL = createLeftAlignedTagRect(editor, cache, font)
    
    if (occupied.none(rectL::intersects)) {
      return (rectL to TagAlignment.LEFT).takeIf { boundaries.isOffsetInside(editor, offsetL, cache) }
    }
    
    val rectR = createRightAlignedTagRect(editor, cache, font)
    
    if (occupied.none(rectR::intersects)) {
      return (rectR to TagAlignment.RIGHT).takeIf { boundaries.isOffsetInside(editor, offsetR, cache) }
    }
    
    return null
  }
  
  private fun createRightAlignedTagRect(editor: Editor, cache: EditorOffsetCache, font: TagFont): Rectangle {
    val pos = cache.offsetToXY(editor, offsetR)
    val shift = font.editorFontMetrics.charWidth(editor.immutableText[offsetR]) + (font.tagCharWidth * shiftR)
    return Rectangle(pos.x + shift, pos.y, font.tagCharWidth * length, font.lineHeight)
  }
  
  private fun createLeftAlignedTagRect(editor: Editor, cache: EditorOffsetCache, font: TagFont): Rectangle {
    val pos = cache.offsetToXY(editor, offsetL)
    val shift = -(font.tagCharWidth * length)
    return Rectangle(pos.x + shift, pos.y, font.tagCharWidth * length, font.lineHeight)
  }
}
