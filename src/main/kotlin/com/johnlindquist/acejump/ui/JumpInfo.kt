package com.johnlindquist.acejump.ui

import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Finder.isRegex
import com.johnlindquist.acejump.search.Finder.query
import com.johnlindquist.acejump.search.getPointFromIndex
import com.johnlindquist.acejump.search.isFirstCharacterOfLine
import com.johnlindquist.acejump.search.wordBounds
import com.johnlindquist.acejump.ui.AceUI.acejumpHighlightColor
import com.johnlindquist.acejump.ui.AceUI.boxColor
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.editorHighlightColor
import com.johnlindquist.acejump.ui.AceUI.editorText
import com.johnlindquist.acejump.ui.AceUI.fontHeight
import com.johnlindquist.acejump.ui.AceUI.fontWidth
import com.johnlindquist.acejump.ui.AceUI.rectHOffset
import com.johnlindquist.acejump.ui.AceUI.rectHeight
import com.johnlindquist.acejump.ui.JumpInfo.Alignment.*
import java.awt.AlphaComposite.SRC_OVER
import java.awt.AlphaComposite.getInstance
import java.awt.Color.BLACK
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON

class JumpInfo(val tag: String, val index: Int) {
  var srcPoint = editor.getPointFromIndex(index)
  var queryLength = query.length
  var trueOffset = query.length - 1

  // TODO: Clean up this mess.
  init {
    var i = 0
    while (i + 1 < query.length && index + i + 1 < editorText.length &&
      query[i + 1].toLowerCase() == editorText[index + i + 1].toLowerCase()) {
      i++
    }

    trueOffset = i
    queryLength = i + 1
  }

  var tagPoint = editor.getPointFromIndex(index + trueOffset)

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
    font = AceUI.font
    color = BLACK
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
    if (query.isEmpty() || alignment == NONE)
      return

    var tagX = point.x
    val lastQueryChar = query.last()
    var tagWidth = tag.length * fontWidth
    val searchWidth = (trueOffset + 1) * fontWidth
    val indexOfEditorChar = index + query.length - 1

    val editorChar =
      if (indexOfEditorChar < editorText.length)
        editorText[indexOfEditorChar].toLowerCase()
      else
        0.toChar()

    // TODO: Use the built-in find-highlighter
    fun highlightAlreadyTyped() {
      g2d.composite = getInstance(SRC_OVER, 0.40.toFloat())
      g2d.color = acejumpHighlightColor
      if (lastQueryChar == tag.first() && lastQueryChar != editorChar) {
        g2d.fillRoundRect(tagX, point.y, fontWidth, rectHeight, rectHeight - 6, rectHeight - 6)
        tagX += fontWidth
        tagWidth -= fontWidth
      }

//      editor.markupModel.addRangeHighlighter(index, index + trueOffset + 1,
//        HighlighterLayer.SELECTION, highlightStyle, HighlighterTargetArea.EXACT_RANGE)
      g2d.fillRoundRect(srcPoint.x, point.y, searchWidth, rectHeight, rectHeight - 6, rectHeight - 6)
    }

    fun highlightRemaining() {
      g2d.color = editorHighlightColor
      val hasSpaceToTheRight = editorText.length <= index + 1 ||
        editorText[index + 1].isWhitespace()

      if (alignment != RIGHT || hasSpaceToTheRight || isRegex)
        g2d.composite = getInstance(SRC_OVER, 1.toFloat())

      g2d.fillRoundRect(tagX, point.y, tagWidth, rectHeight, rectHeight - 6, rectHeight - 6)
    }

    fun surroundTargetWord() {
      g2d.composite = getInstance(SRC_OVER, 1.toFloat())
      val (wordStart, wordEnd) = editorText.wordBounds(index)
      g2d.color = boxColor

      val xPosition = editor.getPointFromIndex(wordStart).x
      val width = (wordEnd - wordStart) * fontWidth

      if (editorText[index].isLetterOrDigit())
        g2d.drawRoundRect(xPosition, point.y, width, rectHeight, rectHeight - 6, rectHeight - 6)
    }

    highlightAlreadyTyped()
    highlightRemaining()

    if (Finder.targetModeEnabled)
      surroundTargetWord()
  }
}