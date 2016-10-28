package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.johnlindquist.acejump.search.*
import java.awt.*
import java.awt.AlphaComposite.*
import java.awt.Color.*
import java.awt.RenderingHints.*

class JumpInfo(private val tag: String, var query: String, val index: Int,
               val editor: EditorImpl) {
  val document = editor.document.charsSequence
  val line = editor.offsetToVisualPosition(index).line
  var originOffset = editor.offsetToVisualPosition(index)
  var trueOffset = index + query.length
  var tagOffset = editor.offsetToVisualPosition(trueOffset)
  var tagPoint = getPointFromVisualPosition(editor, originOffset).originalPoint
  var srcPoint = getPointFromVisualPosition(editor, originOffset).originalPoint
  var text = renderTag()

  fun renderTag(): String {
    var i = 0
    while (i + 1 < query.length &&
      query[i + 1].toLowerCase() == document[index + i + 1].toLowerCase()) {
      i++
    }

    tagOffset = editor.offsetToVisualPosition(index + i)
    tagPoint = getPointFromVisualPosition(editor, tagOffset).originalPoint
    return tag
  }

  fun paintMe(g2d: Graphics2D, ac: AceCanvas) {
    tagPoint.translate(0, -ac.fbm.hOffset.toInt())
    g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
    g2d.composite = getInstance(SRC_OVER, 0.35.toFloat())

    val (tagX, tagY) = alignTag(ac)
    ac.registerTag(Pair(tagX, tagY), tag)
    if (query.isNotEmpty())
      highlight(ac, g2d, tagX, tagY)

    //just a touch of alpha
    g2d.composite = getInstance(SRC_OVER, 0.85.toFloat())

    //the foreground text
    g2d.font = ac.fbm.font
    g2d.color = BLACK
    g2d.drawString(text.toUpperCase(), tagX, tagY + ac.fbm.fontHeight)
  }

  private fun alignTag(ac: AceCanvas): Pair<Int, Int> {
    val y = tagPoint.y //- ac.fbm.rectHOffset.toInt()
    val x = tagPoint.x + ac.fbm.fontWidth
    val lineOffset = getLengthFromStartToOffset(editor, index + query.length)
    val startOfNextLine = getLeadingCharacterOffset(editor, line + 1)
    val startOfThisLine = getLineStartOffset(editor, line)
    val startOfPrevLine = getLeadingCharacterOffset(editor, line - 1)
    val pLineOffset = getLengthFromStartToOffset(editor, startOfPrevLine)
    val nLineOffset = getLengthFromStartToOffset(editor, startOfNextLine)
    val nextLineLength = getNextLineLength(editor, index)
    val previousLineLength = getPreviousLineLength(editor, index)

    val right = Pair(x, y)
    if (query.isNotEmpty()) {
      val previousCharIndex = Math.max(0, index - 1)
      val previousChar = document[previousCharIndex]
      val previousCharIsOnSameLine = startOfThisLine < previousCharIndex
      val alignTop = Pair(x - ac.fbm.fontWidth, y - ac.fbm.lineHeight)
      val alignBottom = Pair(x - ac.fbm.fontWidth, y + ac.fbm.lineHeight)
      val alignLeft = Pair(srcPoint.x - ac.fbm.fontWidth * (text.length), y)

      if (ac.isFree(alignBottom) && (nextLineLength < lineOffset ||
        nLineOffset > lineOffset)) {
        return alignBottom
      } else if (ac.isFree(alignTop) && (previousLineLength < lineOffset ||
        pLineOffset > lineOffset)) {
        return alignTop
      } else if (previousCharIsOnSameLine && ac.isFree(alignLeft)) {
        return alignLeft
      }
    }
    return right
  }

  private fun highlight(ac: AceCanvas, g2d: Graphics2D, x: Int, y: Int) {
    var tagWidth = text.length * ac.fbm.fontWidth
    var searchWidth = query.length * ac.fbm.fontWidth
    var tagX = x
    val lastQueryChar = query.last()
    val correspondingChar = document[index + query.length - 1].toLowerCase()
    g2d.color = green
    if (lastQueryChar == tag.first() && lastQueryChar != correspondingChar) {
      g2d.fillRect(tagX, y, ac.fbm.fontWidth, ac.fbm.lineHeight.toInt())
      tagX += ac.fbm.fontWidth
      tagWidth -= ac.fbm.fontWidth
      searchWidth -= ac.fbm.fontWidth
    }
    g2d.fillRect(srcPoint.x - 1, tagPoint.y, searchWidth, ac.fbm.lineHeight + 1)
    g2d.color = yellow
    g2d.fillRect(tagX, y, tagWidth, ac.fbm.lineHeight)
  }
}
