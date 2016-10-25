package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.johnlindquist.acejump.search.*
import java.awt.AlphaComposite
import java.awt.AlphaComposite.*
import java.awt.Color
import java.awt.Color.*
import java.awt.Graphics2D
import java.awt.RenderingHints
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

  fun drawRect(g2d: Graphics2D, fbm: AceCanvas.FontBasedMeasurements) {
    val text = renderTag()
    val origin = srcPoint
    val original = tagPoint

    original.translate(0, -fbm.hOffset.toInt())
    g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)

    g2d.composite = getInstance(SRC_OVER, 0.35.toFloat())

    var y = original.y
    var x = original.x
    val lineOffset = getLengthFromStartToOffset(editor, index + query.length)
    val startOfNextLine = getLeadingCharacterOffset(editor, line + 1)
    val startOfPrevLine = getLeadingCharacterOffset(editor, line - 1)
    val pLineOffset = getLengthFromStartToOffset(editor, startOfPrevLine)
    val nLineOffset = getLengthFromStartToOffset(editor, startOfNextLine)
    val nextLineLength = getNextLineLength(editor, index)
    val previousLineLength = getPreviousLineLength(editor, index)

    if (query.isNotEmpty()) {
      if (nextLineLength < lineOffset || nLineOffset > lineOffset) {
        y += fbm.lineHeight.toInt()
        x -= fbm.fontWidth
      } else if (previousLineLength < lineOffset || pLineOffset > lineOffset) {
        y -= fbm.lineHeight.toInt()
        x -= fbm.fontWidth
      } else if (!editor.document.charsSequence[index - 1].isLetterOrDigit()) {
        x = origin.x - fbm.fontWidth * (text.length + 1)
      }
    }

    x += fbm.fontWidth
    val x_adjusted = x
    g2d.color = green
    var tagWidth = fbm.rectWidth + text.length * fbm.fontWidth
    var searchWidth = query.length * fbm.fontWidth
    if (query.isNotEmpty()) {
      if (query.last() == tag.first() && query.last().toLowerCase() !=
        document[index + query.length - 1].toLowerCase()) {
        g2d.fillRect(x - fbm.rectMarginWidth, y - fbm.rectHOffset.toInt(),
          fbm.rectWidth + fbm.fontWidth, fbm.lineHeight.toInt())
        x += fbm.fontWidth
        tagWidth -= fbm.fontWidth
        searchWidth -= fbm.fontWidth
      }
      g2d.fillRect(origin.x - 1,
        original.y - fbm.rectHOffset.toInt() - 1,
        searchWidth, fbm.lineHeight.toInt() + 1)
    }

    g2d.color = yellow
    g2d.fillRect(x - fbm.rectMarginWidth, y - fbm.rectHOffset.toInt(),
      tagWidth, fbm.lineHeight.toInt())

    //just a touch of alpha
    g2d.composite = getInstance(SRC_OVER,
      if (text[0] == ' ') 0.25.toFloat() else 0.85.toFloat())

    //the foreground text
    g2d.font = fbm.font
    g2d.color = BLACK
    g2d.drawString(text.toUpperCase(), x_adjusted, y + fbm.fontHeight)
  }
}
