package org.acejump.view

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.CustomHighlighterRenderer
import com.intellij.openapi.editor.markup.RangeHighlighter
import org.acejump.config.AceConfig
import org.acejump.control.Selector
import org.acejump.label.Tagger.regex
import org.acejump.search.*
import org.acejump.search.JumpMode.TARGET
import org.acejump.view.Marker.Alignment.*
import org.acejump.view.Model.arcD
import org.acejump.view.Model.editor
import org.acejump.view.Model.fontHeight
import org.acejump.view.Model.fontWidth
import org.acejump.view.Model.naturalCaretColor
import org.acejump.view.Model.rectHeight
import org.acejump.view.Model.rectVOffset
import java.awt.*
import java.awt.AlphaComposite.SRC_OVER
import java.awt.AlphaComposite.getInstance
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON
import org.acejump.view.Model.editorText as text

/**
 * Represents the visual component of a tag, which gets painted to the screen.
 * All functionality related to tag highlighting including visual appearance,
 * alignment and painting should be handled here. Tags are "captioned" with two
 * or fewer characters. To select a tag, a user will type the tag's assigned
 * "caption", which will move the caret to a known index in the document.
 *
 * TODO: Clean up this class.
 */

class Marker : CustomHighlighterRenderer {
  val index: Int
  private val query: String
  val tag: String?
  private var srcPoint: Point
  private var queryLength: Int
  private var trueOffset: Int
  private val searchWidth: Int
  private val tgIdx: Int
  private val start: Point
  private val startY: Int
  private var tagPoint: Point
  private var yPos: Int
  private var alignment: Alignment
  private val endsWith: Boolean

  constructor(query: String, tag: String?, index: Int) {
    this.query = query
    this.tag = tag
    this.index = index
    val lastQueryChar = query.last()
    endsWith = tag != null && lastQueryChar == tag[0]
    queryLength = query.length - if(endsWith) 1 else 0
    trueOffset = query.length - 1

    searchWidth = if (regex) 0 else queryLength * fontWidth

    var i = 1
    while (i < query.length && index + i < text.length &&
      query[i].toLowerCase() == text[index + i].toLowerCase()) i++
    trueOffset = i - 1
    queryLength = i

    tgIdx = index + trueOffset

    editor.run {
      start = getPoint(index)
      srcPoint = getPointRelative(index, contentComponent)
      tagPoint = getPointRelative(tgIdx, contentComponent)
    }

    startY = start.y + rectVOffset
    yPos = tagPoint.y + rectVOffset
    alignment = RIGHT
  }

  enum class Alignment { /*TOP, BOTTOM,*/ LEFT, RIGHT, NONE }

  // Called by AceJump (as a renderable element of Canvas)
  fun paintMe(graphics: Graphics) = (graphics as Graphics2D).run {
    setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)

    if (!Finder.skim)
      tag?.alignTag(Canvas)
        ?.apply { Canvas.registerTag(this, tag) }
        ?.let {
          if (alignment == NONE) return
          highlightTag(it); drawTagForeground(it)
        }
  }

  /**
   * Called by IntelliJ Platform as a [CustomHighlighterRenderer]
   */

  override fun paint(editor: Editor, highlight: RangeHighlighter, g: Graphics) =
    (g as Graphics2D).run {
    val tagX = start.x + fontWidth
    val tagWidth = tag?.length?.times(fontWidth) ?: 0

    fun highlightRegex() {
      composite = getInstance(SRC_OVER, 0.40.toFloat())
      val xPos = if (alignment == RIGHT) tagX - fontWidth else start.x
      fillRoundRect(xPos, startY, fontWidth, rectHeight, arcD, arcD)
    }

    setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)

    color = AceConfig.textHighlightColor
    if (regex) highlightRegex()
    else {
      fillRoundRect(start.x, startY, searchWidth, rectHeight, arcD, arcD)
      if (JumpMode.equals(TARGET)) surroundTargetWord()
    }

    if (index == Selector.nearestVisibleMatches().firstOrNull())
      indicateAsNearestMatch()
  }

  private fun Graphics2D.indicateAsNearestMatch() {
    color = naturalCaretColor
    drawLine(start.x, startY, start.x, startY + rectHeight)
  }

  private fun Graphics2D.surroundTargetWord() {
    color = AceConfig.targetModeColor
    val (wordStart, wordEnd) = text.wordBounds(index)

    val xPos = editor.getPoint(wordStart).x
    val wordWidth = (wordEnd - wordStart) * fontWidth

    if (text[index].isLetterOrDigit())
      drawRoundRect(xPos, startY, wordWidth, rectHeight, arcD, arcD)
  }

  private fun Graphics2D.drawTagForeground(tagPosition: Point?) {
    font = Model.font
    color = AceConfig.tagForegroundColor
    composite = getInstance(SRC_OVER, 1.toFloat())

    drawString(tag!!.toUpperCase(), tagPosition!!.x, tagPosition.y + fontHeight)
  }

  // TODO: Fix tag alignment and visibility issues
  // https://github.com/acejump/AceJump/issues/233
  // https://github.com/acejump/AceJump/issues/228
  private fun String.alignTag(canvas: Canvas): Point {
    val xPos = tagPoint.x + fontWidth
//    val top = Point(x - fontWidth, y - fontHeight)
//    val bottom = Point(x - fontWidth, y + fontHeight)
    val left = Point(srcPoint.x - fontWidth * length, yPos)
    val right = Point(xPos, yPos)

    val nextCharIsWhiteSpace = text.length <= index + 1 ||
      text[index + 1].isWhitespace()

    val canAlignRight = canvas.isFree(right)
    val isFirstCharacterOfLine = editor.isFirstCharacterOfLine(index)
    val canAlignLeft = !isFirstCharacterOfLine && canvas.isFree(left)

    alignment = when {
      nextCharIsWhiteSpace -> RIGHT
      isFirstCharacterOfLine -> RIGHT
      canAlignLeft -> LEFT
      canAlignRight -> RIGHT
      else -> NONE
    }

    return when (alignment) {
//      TOP -> top
      LEFT -> left
      RIGHT -> right
//      BOTTOM -> bottom
      NONE -> Point(-1, -1)
    }
  }

  private fun Graphics2D.highlightTag(point: Point?) {
    if (query.isEmpty() || alignment == NONE) return

    var tagX = point!!.x
    var tagWidth = tag?.length?.times(fontWidth) ?: 0
    val charIndex = index + query.length - 1
    val beforeEnd = charIndex < text.length
    val textChar = if (beforeEnd) text[charIndex].toLowerCase() else 0.toChar()

    fun highlightFirst() {
      composite = getInstance(SRC_OVER, 0.40.toFloat())
      color = AceConfig.textHighlightColor

      if (endsWith && query.last() != textChar) {
        fillRoundRect(tagX, yPos, fontWidth, rectHeight, arcD, arcD)
        tagX += fontWidth
        tagWidth -= fontWidth
      }
    }

    fun highlightLast() {
      color = AceConfig.tagBackgroundColor
      if (alignment != RIGHT || text.hasSpaceRight(index) || regex)
        composite = getInstance(SRC_OVER, 1.toFloat())

      fillRoundRect(tagX, yPos, tagWidth, rectHeight, arcD, arcD)
    }

    highlightFirst()
    highlightLast()
  }

  infix fun inside(viewBounds: IntRange) = index in viewBounds
}