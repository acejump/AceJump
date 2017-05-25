package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.johnlindquist.acejump.search.Finder
import com.johnlindquist.acejump.search.Finder.query
import com.johnlindquist.acejump.search.Pattern.Companion.REGEX_PREFIX
import com.johnlindquist.acejump.search.getLineStartOffset
import com.johnlindquist.acejump.search.getPointFromVisualPosition
import com.johnlindquist.acejump.search.wordBounds
import com.johnlindquist.acejump.ui.AceUI.acejumpHighlightColor
import com.johnlindquist.acejump.ui.AceUI.boxColor
import com.johnlindquist.acejump.ui.AceUI.document
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.editorHighlightColor
import com.johnlindquist.acejump.ui.AceUI.fontHeight
import com.johnlindquist.acejump.ui.AceUI.fontWidth
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
  var visualPosition = editor.offsetToVisualPosition(index)
    get() = editor.offsetToVisualPosition(index)
  var queryLength = query.length
  var trueOffset = query.length - 1
  var tagOffset = editor.offsetToVisualPosition(index + trueOffset)
  var tagPoint = editor.getPointFromVisualPosition(visualPosition).originalPoint
  var srcPoint = editor.getPointFromVisualPosition(visualPosition).originalPoint
  var text = renderTag()
  val startOfThisLine = editor.getLineStartOffset(line)

  private var alignment = RIGHT

  enum class Alignment { /*TOP, BOTTOM,*/ LEFT, RIGHT, NONE }

  fun renderTag(): String {
    var i = 0
    while (i + 1 < query.length && index + i + 1 < document.length &&
      query[i + 1].toLowerCase() == document[index + i + 1].toLowerCase()) {
      i++
    }

    trueOffset = i
    queryLength = i + 1
    return tag
  }

  fun paintMe(graphics2D: Graphics2D) = graphics2D.run {
    setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)

    val (tagX, tagY) = alignTag(Canvas)
    Canvas.registerTag(Pair(tagX, tagY), tag)
    highlight(graphics2D, tagX, tagY)

    //just a touch of alpha
    composite = getInstance(SRC_OVER, 1.toFloat())

    //the foreground text
    font = AceUI.font
    color = BLACK
    drawString(text.toUpperCase(), tagX, tagY + fontHeight)
  }

  private fun alignTag(ac: Canvas): Pair<Int, Int> {
    val op = (editor as EditorImpl).visualPositionToXY(visualPosition).y
    val y = tagPoint.y + rectHOffset
    val x = tagPoint.x + fontWidth
//    val top = Pair(x - fontWidth, y - fontHeight)
//    val bottom = Pair(x - fontWidth, y + fontHeight)
    val left = Pair(srcPoint.x - fontWidth * (text.length), y)
    val right = Pair(x, y)

    val nextCharIsWhiteSpace = document.length <= index + 1 ||
      document[index + 1].isWhitespace()

    val canAlignRight = ac.isFree(right)
    val isFirstCharacterOfLine = index == startOfThisLine
    val canAlignLeft = !isFirstCharacterOfLine && ac.isFree(left)

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
        g2d.fillRect(tagX, y, fontWidth, fontHeight + 3)
        tagX += fontWidth
        tagWidth -= fontWidth
      }

//      editor.markupModel.addRangeHighlighter(index, index + trueOffset + 1,
//        HighlighterLayer.SELECTION, highlightStyle, HighlighterTargetArea.EXACT_RANGE)
      g2d.fillRect(srcPoint.x - 1, y, searchWidth, fontHeight + 3)
    }

    fun highlightRemaining() {
      g2d.color = editorHighlightColor
      val hasSpaceToTheRight = document.length <= index + 1 ||
        document[index + 1].isWhitespace()

      if (alignment != RIGHT || hasSpaceToTheRight || isRegex)
        g2d.composite = getInstance(SRC_OVER, 1.toFloat())

      g2d.fillRect(tagX, y, tagWidth, fontHeight + 3)
    }

    fun surroundTargetWord() {
      g2d.composite = getInstance(SRC_OVER, 1.toFloat())
      val (wordStart, wordEnd) = document.wordBounds(index)
      g2d.color = boxColor

      val startPoint = editor.offsetToVisualPosition(wordStart)
      val startPointO = editor.getPointFromVisualPosition(startPoint)
      val xPosition = startPointO.originalPoint.x
      val width = (wordEnd - wordStart) * fontWidth

      if (document[index].isLetterOrDigit())
        g2d.drawRect(xPosition, y, width, fontHeight + 3)
    }

    highlightAlreadyTyped()
    highlightRemaining()

    if (Finder.targetModeEnabled)
      surroundTargetWord()
  }
}