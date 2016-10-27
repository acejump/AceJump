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

  fun paintMe(g2d: Graphics2D, fbm: AceCanvas.FontBasedMeasurements) {
    tagPoint.translate(0, -fbm.hOffset.toInt())
    g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
    g2d.composite = getInstance(SRC_OVER, 0.35.toFloat())

    val (tagY, tagX) = alignTag(fbm)
    if (query.isNotEmpty())
      highlight(fbm, g2d, tagX, tagY)

    //just a touch of alpha
    g2d.composite = getInstance(SRC_OVER, 0.85.toFloat())

    //the foreground text
    g2d.font = fbm.font
    g2d.color = BLACK
    g2d.drawString(text.toUpperCase(), tagX, tagY + fbm.fontHeight)
  }

  private fun alignTag(fbm: AceCanvas.FontBasedMeasurements): Pair<Int, Int> {
    var tagY = tagPoint.y
    var tagX = tagPoint.x
    val lineOffset = getLengthFromStartToOffset(editor, index + query.length)
    val startOfNextLine = getLeadingCharacterOffset(editor, line + 1)
    val startOfPrevLine = getLeadingCharacterOffset(editor, line - 1)
    val pLineOffset = getLengthFromStartToOffset(editor, startOfPrevLine)
    val nLineOffset = getLengthFromStartToOffset(editor, startOfNextLine)
    val nextLineLength = getNextLineLength(editor, index)
    val previousLineLength = getPreviousLineLength(editor, index)

    if (query.isNotEmpty()) {
      val previousChar = document[Math.max(0, index - 1)]
      if (previousChar.isWhitespace()) {
        tagX = srcPoint.x - fbm.fontWidth * (text.length + 1)
      } else if (nextLineLength < lineOffset || nLineOffset > lineOffset) {
        tagY += fbm.lineHeight
        tagX -= fbm.fontWidth
      } else if (previousLineLength < lineOffset || pLineOffset > lineOffset) {
        tagY -= fbm.lineHeight
        tagX -= fbm.fontWidth
      } else if (!previousChar.isLetterOrDigit()) {
        tagX = srcPoint.x - fbm.fontWidth * (text.length + 1)
      }
    }
    tagY -= fbm.rectHOffset.toInt()
    tagX += fbm.fontWidth
    return Pair(tagY, tagX)
  }

  private fun highlight(fbm: AceCanvas.FontBasedMeasurements,
                        g2d: Graphics2D, x: Int, y: Int) {
    var tagWidth = text.length * fbm.fontWidth
    var searchWidth = query.length * fbm.fontWidth
    var tagX = x
    val lastQueryChar = query.last()
    val correspondingChar = document[index + query.length - 1].toLowerCase()
    g2d.color = green
    if (lastQueryChar == tag.first() && lastQueryChar != correspondingChar) {
      g2d.fillRect(tagX, y, fbm.fontWidth, fbm.lineHeight.toInt())
      tagX += fbm.fontWidth
      tagWidth -= fbm.fontWidth
      searchWidth -= fbm.fontWidth
    }
    g2d.fillRect(srcPoint.x - 1, tagPoint.y, searchWidth, fbm.lineHeight + 1)
    g2d.color = yellow
    g2d.fillRect(tagX, y, tagWidth, fbm.lineHeight)
  }
}
