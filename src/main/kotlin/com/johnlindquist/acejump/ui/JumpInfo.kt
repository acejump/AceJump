package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.johnlindquist.acejump.search.getPointFromVisualPosition
import java.awt.*

class JumpInfo(private val tag: String, val search: String, val index: Int, val editor: EditorImpl) {
  val window = editor.document.charsSequence
  val source: String = window.substring(index, index + tag.length)
  val tagOffset = editor.offsetToVisualPosition(index)
  val tagPoint = getPointFromVisualPosition(editor, tagOffset).originalPoint
  val searchOffset = editor.offsetToVisualPosition(index - search.length)
  val searchPoint = getPointFromVisualPosition(editor, searchOffset).originalPoint

  fun renderTag(): String {
    return tag.mapIndexed { i, c -> if (source[i] == c) ' ' else c }.joinToString { "" }
  }

  fun getStartOfSearch(): Point {
    return getPointFromVisualPosition(editor, editor.offsetToVisualPosition(index)).originalPoint
  }

  fun drawRect(g2d: Graphics2D, fbm: AceCanvas.FontBasedMeasurements, colors: Pair<Color, Color>) {
    val text = renderTag()
    val originalPoint = tagPoint
    val backgroundColor = if (text[0] == ' ') Color.YELLOW else colors.first
    val foregroundColor = if (text[0] == ' ') Color.YELLOW else colors.second

    originalPoint.translate(0, -fbm.hOffset.toInt())

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    //a slight border for "pop" against the background
    g2d.color = backgroundColor

    if (text.length == 2) {
      g2d.drawRect(originalPoint.x - fbm.rectMarginWidth - 1,
        originalPoint.y - fbm.rectHOffset.toInt() - 1,
        fbm.rectWidth + fbm.fontWidth + 5, fbm.lineHeight.toInt() + 1)
    } else {
      g2d.drawRect(originalPoint.x - fbm.rectMarginWidth - 1,
        originalPoint.y - fbm.rectHOffset.toInt() - 1,
        fbm.rectWidth + 1, fbm.lineHeight.toInt() + 1)
    }

    //the background rectangle
    g2d.color = foregroundColor

    if (text.length == 2) {
      g2d.fillRect(originalPoint.x - fbm.rectMarginWidth,
        originalPoint.y - fbm.rectHOffset.toInt(),
        fbm.rectWidth + fbm.fontWidth + 5, fbm.lineHeight.toInt())
    } else {
      g2d.fillRect(originalPoint.x - fbm.rectMarginWidth,
        originalPoint.y - fbm.rectHOffset.toInt(),
        fbm.rectWidth, fbm.lineHeight.toInt())
    }

    //just a touch of alpha
    g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
      if (text[0] == ' ') 0.25.toFloat() else 0.85.toFloat())

    //the foreground text
    g2d.font = fbm.font
    g2d.color = Color.BLACK
    g2d.drawString(text.toUpperCase(), originalPoint.x, originalPoint.y + fbm.fontHeight)
  }
}
