package com.johnlindquist.acejump.ui

import com.johnlindquist.acejump.search.*
import com.johnlindquist.acejump.ui.JumpInfo.Alignment.*
import java.awt.AlphaComposite.SRC_OVER
import java.awt.AlphaComposite.getInstance
import java.awt.Color.*
import java.awt.Graphics2D
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON

class JumpInfo(private val tag: String, var query: String, val index: Int,
               val aceFinder: AceFinder) {
  val editor = aceFinder.editor
  val document = editor.document.charsSequence
  val isRegex = query.first() == Pattern.REGEX_PREFIX
  val line = editor.offsetToVisualLine(index)
  var originOffset = editor.offsetToVisualPosition(index)
  var queryLength = query.length
  var trueOffset = query.length - 1
  var tagOffset = editor.offsetToVisualPosition(index + trueOffset)
  var tagPoint = getPointFromVisualPosition(editor, originOffset).originalPoint
  var srcPoint = getPointFromVisualPosition(editor, originOffset).originalPoint
  var text = renderTag()

  private var alignment: Alignment = RIGHT

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
    tagPoint = getPointFromVisualPosition(editor, tagOffset).originalPoint
    return tag
  }

  fun paintMe(g2d: Graphics2D, ac: AceCanvas) {
    tagPoint.translate(0, -ac.fbm.hOffset.toInt())
    g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)

    val (tagX, tagY) = alignTag(ac)
    ac.registerTag(Pair(tagX, tagY), tag)
    highlight(ac, g2d, tagX, tagY)

    //just a touch of alpha
    g2d.composite = getInstance(SRC_OVER, 1.toFloat())

    //the foreground text
    g2d.font = ac.fbm.font
    g2d.color = BLACK
    g2d.drawString(text.toUpperCase(), tagX, tagY + ac.fbm.fontHeight)
  }

  val lineOffset = getLengthFromStartToOffset(editor, index + queryLength)
  val firstCharOfNextLine = getLeadingCharacterOffset(editor, line + 1)
  val startOfThisLine = getLineStartOffset(editor, line)
  val firstCharPrevLine = getLeadingCharacterOffset(editor, line - 1)
  val previousLineOffset = getLengthFromStartToOffset(editor, firstCharPrevLine)
  val nextLineOffset = getLengthFromStartToOffset(editor, firstCharOfNextLine)
  val nextLineLength = getNextLineLength(editor, index)
  val previousLineLength = getPreviousLineLength(editor, index)
  val topLine = getVisualLineAtTopOfScreen(editor)
  val bottomLine = topLine + getScreenHeight(editor)

  private fun alignTag(ac: AceCanvas): Pair<Int, Int> {
    val y = tagPoint.y - ac.fbm.rectHOffset.toInt()
    val x = tagPoint.x + ac.fbm.fontWidth
    val top = Pair(x - ac.fbm.fontWidth, y - ac.fbm.lineHeight)
    val bottom = Pair(x - ac.fbm.fontWidth, y + ac.fbm.lineHeight)
    val left = Pair(srcPoint.x - ac.fbm.fontWidth * (text.length), y)
    val right = Pair(x, y)

    val canAlignTop = ac.isFree(top) &&
      (topLine..bottomLine).contains(line - 1) &&
      (previousLineLength < lineOffset || previousLineOffset > lineOffset)

    val canAlignBottom = ac.isFree(bottom) &&
      (topLine..bottomLine).contains(line + 1) &&
      (nextLineLength < lineOffset || nextLineOffset > lineOffset)

    val nextCharIsWhiteSpace = document.length <= index + 1 ||
      document[index + 1].isWhitespace()

    val canAlignRight = ac.isFree(right)

    val canAlignLeft = editor.offsetToLogicalPosition(index).column != 0 &&
      ac.isFree(left)
    val isFirstCharacterOfLine = index == startOfThisLine
    alignment = if (nextCharIsWhiteSpace) RIGHT
    else if (isFirstCharacterOfLine)
      RIGHT
    else if (canAlignLeft)
      LEFT
    else if (canAlignRight)
      RIGHT
    else
      NONE

    return when (alignment) {
      TOP -> top
      LEFT -> left
      RIGHT -> right
      BOTTOM -> bottom
      NONE -> Pair(0, 0)
    }
  }

  private fun highlight(ac: AceCanvas, g2d: Graphics2D, x: Int, y: Int) {
    if (query.isEmpty() || alignment == NONE)
      return

    var tagWidth = text.length * ac.fbm.fontWidth
    val searchWidth = (trueOffset + 1) * ac.fbm.fontWidth
    var tagX = x
    val lastQueryChar = query.last()
    val indexOfCorrespondingChar = index + query.length - 1

    val correspondingChar =
      if (indexOfCorrespondingChar < document.length)
        document[indexOfCorrespondingChar].toLowerCase()
      else
        0.toChar()

    fun highlightAlreadyTyped() {
      g2d.composite = getInstance(SRC_OVER, 0.40.toFloat())
      g2d.color = green
      if (lastQueryChar == tag.first() && lastQueryChar != correspondingChar) {
        g2d.fillRect(tagX, y, ac.fbm.fontWidth, ac.fbm.lineHeight)
        tagX += ac.fbm.fontWidth
        tagWidth -= ac.fbm.fontWidth
      }
      g2d.fillRect(srcPoint.x - 1, tagPoint.y, searchWidth, ac.fbm.lineHeight)
    }

    fun highlightRemaining() {
      g2d.color = yellow
      val hasSpaceToTheRight = document.length <= index + 1 ||
        document[index + 1].isWhitespace()

      if (alignment != RIGHT || hasSpaceToTheRight || isRegex)
        g2d.composite = getInstance(SRC_OVER, 1.toFloat())

      g2d.fillRect(tagX, y, tagWidth, ac.fbm.lineHeight)
    }

    fun surroundTargetWord() {
      val (wordStart, wordEnd) = aceFinder.getWordBounds(index)
      g2d.color = red

      val startPoint = editor.offsetToVisualPosition(wordStart)
      val startPointO = getPointFromVisualPosition(editor, startPoint)
      val xPosition = startPointO.originalPoint.x
      val width = (wordEnd - wordStart) * ac.fbm.fontWidth

      if(document[index].isLetterOrDigit())
        g2d.drawRect(xPosition, y, width, ac.fbm.fontHeight + 3)
    }

    highlightAlreadyTyped()
    highlightRemaining()

    if(aceFinder.targetModeEnabled) {
      surroundTargetWord()
    }
  }
}