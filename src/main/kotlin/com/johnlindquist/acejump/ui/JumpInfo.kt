package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.johnlindquist.acejump.search.*
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Color.*
import java.awt.Graphics2D
import java.awt.RenderingHints

class JumpInfo(private val tag: String, var search: String, val index: Int, val editor: EditorImpl) {
  val window = editor.document.charsSequence
  var offset = index
  val line = editor.offsetToVisualPosition(offset).line
  var originOffset = editor.offsetToVisualPosition(offset)
  var trueOffset = offset + search.length
  var tagOffset = editor.offsetToVisualPosition(trueOffset)
  var tagPoint = getPointFromVisualPosition(editor, originOffset).originalPoint
  var srcPoint = getPointFromVisualPosition(editor, originOffset).originalPoint

  fun renderTag(): String {
    var trueOffset = 0
    var i = 0
    while (i++ < search.length) {
      if (i < search.length && window[index + i].toLowerCase() == search[i].toLowerCase())
        trueOffset++
      else
        break
    }
    tagOffset = editor.offsetToVisualPosition(offset + trueOffset)
    tagPoint = getPointFromVisualPosition(editor, tagOffset).originalPoint
    return tag
  }

  fun drawRect(g2d: Graphics2D, fbm: AceCanvas.FontBasedMeasurements, colors: Pair<Color, Color>) {
    val text = renderTag()
    val origin = srcPoint
    val original = tagPoint

    original.translate(0, -fbm.hOffset.toInt())
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35.toFloat())

    var y = original.y
    var x = original.x
    val lineOffset = getLengthFromStartToOffset(editor, offset + search.length)
    val startOfNextLine = getLeadingCharacterOffset(editor, line + 1)
    val startOfPrevLine = getLeadingCharacterOffset(editor, line - 1)
    val pLineOffset = getLengthFromStartToOffset(editor, startOfPrevLine)
    val nLineOffset = getLengthFromStartToOffset(editor, startOfNextLine)
    if (search.isNotEmpty()) {
      if (getNextLineLength(editor, offset) < lineOffset || nLineOffset > lineOffset) {
        y += fbm.lineHeight.toInt()
        x -= fbm.fontWidth
      } else if (getPreviousLineLength(editor, offset) < lineOffset || pLineOffset > lineOffset) {
        y -= fbm.lineHeight.toInt()
        x -= fbm.fontWidth
      } else if (!editor.document.charsSequence[offset - 1].isLetterOrDigit()) {
        x = origin.x - fbm.fontWidth * (text.length + 1)
      }
    }

    x += fbm.fontWidth
    val x_adjusted = x
    g2d.color = green
    var tagWidth = fbm.rectWidth + text.length * fbm.fontWidth
    var searchWidth = search.length * fbm.fontWidth
    if (search.isNotEmpty()) {
      if (search.last() == tag.first() && search.last().toLowerCase() !=
          editor.document.charsSequence[offset + search.length - 1].toLowerCase()) {
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
    g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
      if (text[0] == ' ') 0.25.toFloat() else 0.85.toFloat())

    //the foreground text
    g2d.font = fbm.font
    g2d.color = BLACK
    g2d.drawString(text.toUpperCase(), x_adjusted, y + fbm.fontHeight)
  }
}
