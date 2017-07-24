package com.johnlindquist.acejump.view

import com.johnlindquist.acejump.config.AceConfig.Companion.settings
import com.johnlindquist.acejump.search.*
import com.johnlindquist.acejump.search.Finder.isRegex
import com.johnlindquist.acejump.view.Marker.Alignment.*
import com.johnlindquist.acejump.view.Model.arcD
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.fontHeight
import com.johnlindquist.acejump.view.Model.fontWidth
import com.johnlindquist.acejump.view.Model.rectHOffset
import com.johnlindquist.acejump.view.Model.rectHeight
import java.awt.AlphaComposite.SRC_OVER
import java.awt.AlphaComposite.getInstance
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON
import com.johnlindquist.acejump.view.Model.editorText as text

/**
 * All functionality related to tag highlighting (ie. the visual overlay which
 * AceJump paints when displaying search results). Tags are "captioned" with two
 * or fewer characters. To select a tag, a user will type the tag's assigned
 * caption, which will move the cursor to a known index in the document.
 */

class Marker(val query: String, val tag: String?, val index: Int) {
  private var srcPoint = editor.getPointFromIndex(index)
  private var queryLength = query.length
  private var trueOffset = query.length - 1

  // TODO: Clean up this mess.
  init {
    var i = 1
    while (i < query.length && index + i < text.length &&
      query[i].toLowerCase() == text[index + i].toLowerCase()) i++

    trueOffset = i - 1
    queryLength = i
  }

  private val searchWidth = queryLength * fontWidth
  private var tagPoint = editor.getPointFromIndex(index + trueOffset)
  private var yPosition = tagPoint.y
  private var alignment = RIGHT

  enum class Alignment { /*TOP, BOTTOM,*/ LEFT, RIGHT, NONE }

  fun paintMe(graphics2D: Graphics2D) = graphics2D.run {
    setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)

    tag?.alignTag(Canvas)
      ?.apply { Canvas.registerTag(this, tag) }
      ?.let { highlightTag(it); drawTagForeground(it) }

    if (Finder.isRegex)
      highlightText()
  }

  private fun Graphics2D.highlightText() {
    color = settings.textHighlightColor
    composite = getInstance(SRC_OVER, 0.40.toFloat())

    fillRoundRect(srcPoint.x, yPosition, searchWidth, rectHeight, arcD, arcD)
  }

  private fun Graphics2D.drawTagForeground(tagPosition: Point?) {
    font = Model.font
    color = settings.tagForegroundColor
    composite = getInstance(SRC_OVER, 1.toFloat())

    drawString(tag!!.toUpperCase(), tagPosition!!.x, tagPosition.y + fontHeight)
  }

  private fun String.alignTag(canvas: Canvas): Point {
    val y = tagPoint.y + rectHOffset
    val x = tagPoint.x + fontWidth
//    val top = Point(x - fontWidth, y - fontHeight)
//    val bottom = Point(x - fontWidth, y + fontHeight)
    val left = Point(srcPoint.x - fontWidth * length, y)
    val right = Point(x, y)

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
      NONE -> Point(0, 0)
    }
  }

  private fun Graphics2D.highlightTag(point: Point?) {
    if (query.isEmpty() || alignment == NONE) return

    var tagX = point?.x
    val lastQueryChar = query.last()
    var tagWidth = tag?.length?.times(fontWidth) ?: 0
    val charIndex = index + query.length - 1
    val beforeEnd = charIndex < text.length
    val textChar = if (beforeEnd) text[charIndex].toLowerCase() else 0.toChar()

    // TODO: Use the built-in find-highlighter
    fun highlightAlreadyTyped() {
      composite = getInstance(SRC_OVER, 0.40.toFloat())
      color = settings.textHighlightColor

      if (tag != null && lastQueryChar == tag.first() && lastQueryChar != textChar) {
        fillRoundRect(tagX!!, yPosition, fontWidth, rectHeight, arcD, arcD)
        tagX += fontWidth
        tagWidth -= fontWidth
      }
    }

    fun highlightRemaining() {
      color = settings.tagBackgroundColor
      if (alignment != RIGHT || text.hasSpaceRight(index) || isRegex)
        composite = getInstance(SRC_OVER, 1.toFloat())

      fillRoundRect(tagX!!, yPosition, tagWidth, rectHeight, arcD, arcD)
    }

    fun surroundTargetWord() {
      color = settings.targetModeColor
      composite = getInstance(SRC_OVER, 1.toFloat())
      val (wordStart, wordEnd) = text.wordBounds(index)

      val xPosition = editor.getPointFromIndex(wordStart).x
      val wordWidth = (wordEnd - wordStart) * fontWidth

      if (text[index].isLetterOrDigit())
        drawRoundRect(xPosition, yPosition, wordWidth, rectHeight, arcD, arcD)
    }

    highlightAlreadyTyped()
    highlightRemaining()

    if (Finder.targetModeEnabled) surroundTargetWord()
  }
}