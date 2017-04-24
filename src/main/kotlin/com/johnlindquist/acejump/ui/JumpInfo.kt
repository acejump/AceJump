package com.johnlindquist.acejump.ui

import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Finder.query
import com.johnlindquist.acejump.search.Pattern
import com.johnlindquist.acejump.search.Pattern.Companion.REGEX_PREFIX
import com.johnlindquist.acejump.search.getLineStartOffset
import com.johnlindquist.acejump.search.getPointFromVisualPosition
import com.johnlindquist.acejump.ui.AceUI.acejumpHighlightColor
import com.johnlindquist.acejump.ui.AceUI.boxColor
import com.johnlindquist.acejump.ui.AceUI.document
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.editorHighlightColor
import com.johnlindquist.acejump.ui.AceUI.font
import com.johnlindquist.acejump.ui.AceUI.fontHeight
import com.johnlindquist.acejump.ui.AceUI.fontWidth
import com.johnlindquist.acejump.ui.AceUI.lineHeight
import com.johnlindquist.acejump.ui.AceUI.rectHOffset
import com.johnlindquist.acejump.ui.JumpInfo.Alignment.*
import java.awt.AlphaComposite.SRC_OVER
import java.awt.AlphaComposite.getInstance
import java.awt.Color.BLACK
import java.awt.Graphics2D
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON

class JumpInfo(val tag: String, val index: Int) {
  val isRegex = query.first() == REGEX_PREFIX
  val line = editor.offsetToVisualPosition(index).line
  var originOffset = editor.offsetToVisualPosition(index)
  var queryLength = query.length
  var trueOffset = query.length - 1
  var tagOffset = editor.offsetToVisualPosition(index + trueOffset)
  var tagPoint = editor.getPointFromVisualPosition(originOffset).originalPoint
  var srcPoint = editor.getPointFromVisualPosition(originOffset).originalPoint
  var text = renderTag()

  private var alignment = RIGHT

  enum class Alignment { TOP, BOTTOM, LEFT, RIGHT, NONE }

  fun renderTag(): String {
    var i = 0
    while (i + 1 < query.length && index + i + 1 < document.length &&
      query[i + 1].toLowerCase() == document[index + i + 1].toLowerCase()) {
      i++
    }

    trueOffset = i
    queryLength = i + 1
    tagOffset = editor.offsetToVisualPosition(index + trueOffset)
    tagPoint = editor.getPointFromVisualPosition(tagOffset).originalPoint
    return tag
  }

  fun paintMe(g2d: Graphics2D) {
    g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)

    val (tagX, tagY) = alignTag(Canvas)
    Canvas.registerTag(Pair(tagX, tagY), tag)
    highlight(g2d, tagX, tagY)

    //just a touch of alpha
    g2d.composite = getInstance(SRC_OVER, 1.toFloat())

    //the foreground text
    g2d.font = font
    g2d.color = BLACK
    g2d.drawString(text.toUpperCase(), tagX, tagPoint.y - rectHOffset + fontHeight)
  }

  val startOfThisLine = editor.getLineStartOffset(line)

  private fun alignTag(ac: Canvas): Pair<Int, Int> {
    val y = tagPoint.y - rectHOffset
    val x = tagPoint.x + fontWidth
    val top = Pair(x - fontWidth, y - lineHeight)
    val bottom = Pair(x - fontWidth, y + lineHeight)
    val left = Pair(srcPoint.x - fontWidth * (text.length), y)
    val right = Pair(x, y)

    val nextCharIsWhiteSpace = document.length <= index + 1 ||
      document[index + 1].isWhitespace()

    val canAlignRight = ac.isFree(right)
    val canAlignLeft =
      editor.offsetToLogicalPosition(index).column != 0 && ac.isFree(left)
    val isFirstCharacterOfLine = index == startOfThisLine
    alignment = if (nextCharIsWhiteSpace) RIGHT
    else if (isFirstCharacterOfLine) RIGHT
    else if (canAlignLeft) LEFT
    else if (canAlignRight) RIGHT
    else NONE

    return when (alignment) {
      TOP -> top
      LEFT -> left
      RIGHT -> right
      BOTTOM -> bottom
      NONE -> Pair(0, 0)
    }
  }

  private fun highlight(g2d: Graphics2D, x: Int, y: Int) {
    if (query.isEmpty() || alignment == NONE)
      return

    var tagX = x
    val lastQueryChar = query.last()
    var tagWidth = text.length * fontWidth
    val searchWidth = (trueOffset + 1) * fontWidth
    val indexOfEditorChar = index + query.length - 1

    val editorChar =
      if (indexOfEditorChar < document.length)
        document[indexOfEditorChar].toLowerCase()
      else
        0.toChar()

    // TODO: Use the built-in find-highlighter
    fun highlightAlreadyTyped() {
      g2d.composite = getInstance(SRC_OVER, 0.40.toFloat())
      g2d.color = acejumpHighlightColor
      if (lastQueryChar == tag.first() && lastQueryChar != editorChar) {
        g2d.fillRect(tagX, tagPoint.y - rectHOffset, fontWidth, fontHeight + 3)
        tagX += fontWidth
        tagWidth -= fontWidth
      }

      g2d.fillRect(srcPoint.x - 1, tagPoint.y - rectHOffset, searchWidth, fontHeight + 3)
    }

    fun highlightRemaining() {
      g2d.color = editorHighlightColor
      val hasSpaceToTheRight = document.length <= index + 1 ||
        document[index + 1].isWhitespace()

      if (alignment != RIGHT || hasSpaceToTheRight || isRegex)
        g2d.composite = getInstance(SRC_OVER, 1.toFloat())

      g2d.fillRect(tagX, tagPoint.y - rectHOffset, tagWidth, fontHeight + 3)
    }

    fun surroundTargetWord() {
      g2d.composite = getInstance(SRC_OVER, 1.toFloat())
      val (wordStart, wordEnd) = Finder.getWordBounds(index)
      g2d.color = boxColor

      val startPoint = editor.offsetToVisualPosition(wordStart)
      val startPointO = editor.getPointFromVisualPosition(startPoint)
      val xPosition = startPointO.originalPoint.x
      val width = (wordEnd - wordStart) * fontWidth

      if (document[index].isLetterOrDigit())
        g2d.drawRect(xPosition, tagPoint.y - rectHOffset, width, fontHeight + 3)
    }

    highlightAlreadyTyped()
    highlightRemaining()

    if (Finder.targetModeEnabled)
      surroundTargetWord()
  }
}