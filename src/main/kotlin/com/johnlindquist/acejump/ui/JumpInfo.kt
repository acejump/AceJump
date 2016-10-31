package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.johnlindquist.acejump.search.*
import com.johnlindquist.acejump.ui.JumpInfo.Alignment.*
import java.awt.*
import java.awt.AlphaComposite.*
import java.awt.Color.*
import java.awt.RenderingHints.*

class JumpInfo(private val tag: String, var query: String, val index: Int,
               val editor: EditorImpl) {
  val document = editor.document.charsSequence
  val line = editor.offsetToVisualPosition(index).line
  var originOffset = editor.offsetToVisualPosition(index)
  var queryLength = query.length
  var trueOffset = query.length - 1
  var tagOffset = editor.offsetToVisualPosition(index + trueOffset)
  var tagPoint = getPointFromVisualPosition(editor, originOffset).originalPoint
  var srcPoint = getPointFromVisualPosition(editor, originOffset).originalPoint
  var text = renderTag()

  private var alignment: Alignment = ALIGN_RIGHT
  enum class Alignment { ALIGN_TOP, ALIGN_BOTTOM, ALIGN_LEFT, ALIGN_RIGHT }

  fun renderTag(): String {
    var i = 0
    while (i + 1 < query.length &&
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

  private fun alignTag(ac: AceCanvas): Pair<Int, Int> {
    val y = tagPoint.y - ac.fbm.rectHOffset.toInt()
    val x = tagPoint.x + ac.fbm.fontWidth
    val lineOffset = getLengthFromStartToOffset(editor, index + queryLength)
    val startOfNextLine = getLeadingCharacterOffset(editor, line + 1)
    val startOfThisLine = getLineStartOffset(editor, line)
    val startOfPrevLine = getLeadingCharacterOffset(editor, line - 1)
    val previousLineOffset = getLengthFromStartToOffset(editor, startOfPrevLine)
    val nextLineOffset = getLengthFromStartToOffset(editor, startOfNextLine)
    val nextLineLength = getNextLineLength(editor, index)
    val previousLineLength = getPreviousLineLength(editor, index)
    val topLine = getVisualLineAtTopOfScreen(editor)
    val bottomLine = topLine + getScreenHeight(editor)
    val previousCharIndex = Math.max(0, index - 1)
    val alignTop = Pair(x - ac.fbm.fontWidth, y - ac.fbm.lineHeight)
    val alignBottom = Pair(x - ac.fbm.fontWidth, y + ac.fbm.lineHeight)
    val alignLeft = Pair(srcPoint.x - ac.fbm.fontWidth * (text.length), y)
    val alignRight = Pair(x, y)

    val canAlignTop =
      ac.isFree(alignTop) &&
        (topLine..bottomLine).contains(line - 1) &&
        (previousLineLength < lineOffset || previousLineOffset > lineOffset)

    val canAlignBottom =
      ac.isFree(alignBottom) &&
        (topLine..bottomLine).contains(line + 1) &&
        (nextLineLength < lineOffset || nextLineOffset > lineOffset)

    val canAlignLeft =
      startOfThisLine < previousCharIndex && ac.isFree(alignLeft)

    if (query.isNotEmpty())
      if (canAlignBottom) {
        alignment = ALIGN_BOTTOM
        return alignBottom
      } else if (canAlignTop) {
        alignment = ALIGN_TOP
        return alignTop
      } else if (canAlignLeft) {
        alignment = ALIGN_LEFT
        return alignLeft
      }

    return alignRight
  }

  private fun highlight(ac: AceCanvas, g2d: Graphics2D, x: Int, y: Int) {
    if (query.isEmpty())
      return

    var tagWidth = text.length * ac.fbm.fontWidth
    var searchWidth = (trueOffset + 1) * ac.fbm.fontWidth
    var tagX = x
    val lastQueryChar = query.last()
    val correspondingChar = document[index + query.length - 1].toLowerCase()
    g2d.composite = getInstance(SRC_OVER, 0.40.toFloat())

    fun highlightAlreadyTyped() {
      g2d.color = green
      if (lastQueryChar == tag.first() && lastQueryChar != correspondingChar) {
        g2d.fillRect(tagX, y, ac.fbm.fontWidth, ac.fbm.lineHeight.toInt())
        tagX += ac.fbm.fontWidth
        tagWidth -= ac.fbm.fontWidth
      }
      g2d.fillRect(srcPoint.x - 1, tagPoint.y, searchWidth, ac.fbm.lineHeight)
    }

    fun highlightRemaining() {
      g2d.color = yellow
      if(alignment != ALIGN_RIGHT)
        g2d.composite = getInstance(SRC_OVER, 1.toFloat())

      g2d.fillRect(tagX, y, tagWidth, ac.fbm.lineHeight)
    }

    highlightAlreadyTyped()
    highlightRemaining()
  }
}
