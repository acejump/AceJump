package com.johnlindquist.acejump.view

import com.johnlindquist.acejump.config.AceConfig.settings
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Finder.isRegex
import com.johnlindquist.acejump.search.Finder.query
import com.johnlindquist.acejump.search.getPointFromIndex
import com.johnlindquist.acejump.search.isFirstCharacterOfLine
import com.johnlindquist.acejump.search.wordBounds
import com.johnlindquist.acejump.view.Marker.Alignment.*
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
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

class Marker(val tag: String, val index: Int) {
  private var srcPoint = editor.getPointFromIndex(index)
  private var queryLength = query.length
  private var trueOffset = query.length - 1

  // TODO: Clean up this mess.
  init {
    var i = 1
    while (i < query.length && index + i < editorText.length &&
      query[i].toLowerCase() == editorText[index + i].toLowerCase()) i++

    trueOffset = i - 1
    queryLength = i
  }

  private var tagPoint = editor.getPointFromIndex(index + trueOffset)

  private var alignment = RIGHT

  enum class Alignment { /*TOP, BOTTOM,*/ LEFT, RIGHT, NONE }

  fun paintMe(graphics2D: Graphics2D) = graphics2D.run {
    setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)

    val tagPosition = alignTag(Canvas)
    Canvas.registerTag(tagPosition, tag)
    highlight(graphics2D, tagPosition)

    //just a touch of alpha
    composite = getInstance(SRC_OVER, 1.toFloat())

    //the foreground text
    font = Model.font
    color = settings.tagForegroundColor
    drawString(tag.toUpperCase(), tagPosition.x, tagPosition.y + fontHeight)
  }

  private fun alignTag(canvas: Canvas): Point {
    val y = tagPoint.y + rectHOffset
    val x = tagPoint.x + fontWidth
//    val top = Point(x - fontWidth, y - fontHeight)
//    val bottom = Point(x - fontWidth, y + fontHeight)
    val left = Point(srcPoint.x - fontWidth * (tag.length), y)
    val right = Point(x, y)

    val nextCharIsWhiteSpace = editorText.length <= index + 1 ||
      editorText[index + 1].isWhitespace()

    val canAlignRight = canvas.isFree(right)
    val isFirstCharacterOfLine = editor.isFirstCharacterOfLine(index)
    val canAlignLeft = !isFirstCharacterOfLine && canvas.isFree(left)

    alignment = if (nextCharIsWhiteSpace) RIGHT
    else if (isFirstCharacterOfLine) RIGHT
    else if (canAlignLeft) LEFT
    else if (canAlignRight) RIGHT
    else NONE

    return when (alignment) {
//      TOP -> top
      LEFT -> left
      RIGHT -> right
//      BOTTOM -> bottom
      NONE -> Point(0, 0)
    }
  }

  private fun highlight(g2d: Graphics2D, point: Point) {
    if (query.isEmpty() || alignment == NONE) return

    var tagX = point.x
    val lastQueryChar = query.last()
    var tagWidth = tag.length * fontWidth
    val searchWidth = (trueOffset + 1) * fontWidth
    val charIndex = index + query.length - 1
    val lessThanLen = charIndex < editorText.length
    val editorChar = if (lessThanLen) editorText[charIndex].toLowerCase() else 0.toChar()

    // TODO: Use the built-in find-highlighter
    fun highlightAlreadyTyped() {
      g2d.composite = getInstance(SRC_OVER, 0.40.toFloat())
      g2d.color = settings.textHighLightColor
      if (lastQueryChar == tag.first() && lastQueryChar != editorChar) {
        g2d.fillRoundRect(tagX, point.y, fontWidth, rectHeight, rectHeight - 6, rectHeight - 6)
        tagX += fontWidth
        tagWidth -= fontWidth
      }

      g2d.fillRoundRect(srcPoint.x, point.y, searchWidth, rectHeight, rectHeight - 6, rectHeight - 6)
    }

    fun highlightRemaining() {
      g2d.color = settings.tagBackgroundColor
      val hasSpaceToTheRight = editorText.length <= index + 1 ||
        editorText[index + 1].isWhitespace()

      if (alignment != RIGHT || hasSpaceToTheRight || isRegex)
        g2d.composite = getInstance(SRC_OVER, 1.toFloat())

      g2d.fillRoundRect(tagX, point.y, tagWidth, rectHeight, rectHeight - 6, rectHeight - 6)
    }

    fun surroundTargetWord() {
      g2d.composite = getInstance(SRC_OVER, 1.toFloat())
      val (wordStart, wordEnd) = editorText.wordBounds(index)
      g2d.color = settings.targetModeColor

      val xPosition = editor.getPointFromIndex(wordStart).x
      val width = (wordEnd - wordStart) * fontWidth

      if (editorText[index].isLetterOrDigit())
        g2d.drawRoundRect(xPosition, point.y, width, rectHeight, rectHeight - 6, rectHeight - 6)
    }

    highlightAlreadyTyped()
    highlightRemaining()

    if (Finder.targetModeEnabled) surroundTargetWord()
  }
}