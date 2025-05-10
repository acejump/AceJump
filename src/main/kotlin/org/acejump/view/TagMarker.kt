package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.ui.ColorUtil
import com.intellij.ui.JreHiDpiUtil
import com.intellij.ui.scale.JBUIScale
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries.VISIBLE_ON_SCREEN
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
class TagMarker(
  private val tag: String,
  val offsetL: Int,
  val offsetR: Int,
  private val shiftR: Int,
  private val hasSpaceRight: Boolean
) {
  private val length = tag.length

  companion object {
    private const val ARC = 1

    /**
     * Creates a new tag, precomputing some information about the nearby characters to reduce rendering overhead. If the last typed
     * character ([literalQueryText]) matches the first [tag] character, only the second [tag] character is displayed.
     */
    fun create(editor: Editor, tag: String, offset: Int, literalQueryText: String?): TagMarker {
      val chars = editor.immutableText
      val matching = literalQueryText?.let { chars.countMatchingCharacters(offset, it) } ?: 0
      val hasSpaceRight = offset + 1 >= chars.length || chars[offset + 1].isWhitespace()

      val displayedTag = if (literalQueryText != null && literalQueryText.last().equals(tag.first(), ignoreCase = true))
        tag.drop(1).uppercase()
      else
        tag.uppercase()

      return TagMarker(displayedTag, offset, offset + max(0, matching - 1), tag.length - displayedTag.length, hasSpaceRight)
    }

    /**
     * Renders the tag background.
     */
    private fun drawHighlight(g: Graphics2D, rect: Rectangle, color: Color) {
      g.color = color
      
      // Workaround for misalignment on high DPI screens.
      if (JreHiDpiUtil.isJreHiDPI(g)) {
        g.translate(0.0, -0.5)
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height + 1, ARC, ARC)
        g.translate(0.0, 0.5)
      }
      else {
        g.fillRoundRect(rect.x, rect.y, rect.width, rect.height + 1, ARC, ARC)
      }
    }

    /**
     * Renders the tag text.
     */
    private fun drawForeground(g: Graphics2D, font: TagFont, point: Point, text: String) {
      val x = point.x + 2
      val y = point.y + font.baselineDistance

      g.font = font.tagFont

      if (!ColorUtil.isDark(AceConfig.tagForegroundColor)) {
        g.color = Color(0F, 0F, 0F, 0.35F)
        g.drawString(text, x + 1, y + 1)
      }

      g.color = AceConfig.tagForegroundColor
      g.drawString(text, x, y)
    }
    
    private fun isLineEnding(char: Char): Boolean {
      return char == '\n' || char == '\r'
    }
  }

  /**
   * Returns true if the left-aligned offset is in the range. Use to cull tags outside visible range.
   * Only the left offset is checked, because if the tag was right-aligned on the last index of the range, it would not be visible anyway.
   */
  fun isOffsetInRange(range: IntRange): Boolean = offsetL in range

  /**
   * Paints the tag, taking into consideration visual space around characters in the editor, as well as all other previously painted tags.
   * Returns a rectangle indicating the area where the tag was rendered, or null if the tag could not be rendered due to overlap.
   */
  fun paint(g: Graphics2D, editor: Editor, cache: EditorOffsetCache, font: TagFont, occupied: MutableList<Rectangle>): Rectangle? {
    val rect = alignTag(editor, cache, font, occupied) ?: return null

    drawHighlight(g, rect, AceConfig.tagBackgroundColor)
    drawForeground(g, font, rect.location, tag)

    occupied.add(JBUIScale.scale(2).let { Rectangle(rect.x - it, rect.y, rect.width + (2 * it), rect.height) })
    return rect
  }

  private fun alignTag(editor: Editor, cache: EditorOffsetCache, font: TagFont, occupied: List<Rectangle>): Rectangle? {
    val boundaries = VISIBLE_ON_SCREEN

    if (hasSpaceRight || offsetL !in 1 until editor.document.textLength || isLineEnding(editor.immutableText[offsetL - 1])) {
      val rectR = createRightAlignedTagRect(editor, cache, font)
      return rectR.takeIf { boundaries.isOffsetInside(editor, offsetR, cache) && occupied.none(rectR::intersects) }
    }

    val rectL = createLeftAlignedTagRect(editor, cache, font)
    if (rectL.x >= 0 && occupied.none(rectL::intersects))
      return rectL.takeIf { boundaries.isOffsetInside(editor, offsetL, cache) }

    val rectR = createRightAlignedTagRect(editor, cache, font)
    if (occupied.none(rectR::intersects))
      return rectR.takeIf { boundaries.isOffsetInside(editor, offsetR, cache) }

    return null
  }

  private fun createRightAlignedTagRect(editor: Editor, cache: EditorOffsetCache, font: TagFont): Rectangle {
    val pos = cache.offsetToXY(editor, offsetR)
  
    val char = if (offsetR >= editor.document.textLength)
      ' ' // Use the width of a space on the last line.
    else editor.immutableText[offsetR].let {
      // Use the width of a space on empty lines.
      if (isLineEnding(it)) ' ' else it
    }
  
    val shift = font.editorFontMetrics.charWidth(char) + (font.tagCharWidth * shiftR)
    return Rectangle(pos.x + shift, pos.y, (font.tagCharWidth * length) + 4, font.lineHeight)
  }

  private fun createLeftAlignedTagRect(editor: Editor, cache: EditorOffsetCache, font: TagFont): Rectangle {
    val pos = cache.offsetToXY(editor, offsetL)
    val shift = -(font.tagCharWidth * length)
    return Rectangle(pos.x + shift - 4, pos.y, (font.tagCharWidth * length) + 4, font.lineHeight)
  }
}
