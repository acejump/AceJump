package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.johnlindquist.acejump.search.getPointFromVisualPosition
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Color.BLACK
import java.awt.Color.yellow
import java.awt.Graphics2D
import java.awt.RenderingHints

class JumpInfo(private val tag: String, val search: String, val index: Int, val editor: EditorImpl) {
  val window = editor.document.charsSequence
  val source: String = window.substring(index, index + tag.length)
  var offset = index - search.length + tag.length
  val tagOffset = editor.offsetToVisualPosition(offset)
  val tagPoint = getPointFromVisualPosition(editor, tagOffset).originalPoint

  fun renderTag() = tag.mapIndexed { i, c ->
    if (source.isEmpty() || source[i] == c) ' ' else c
  }.joinToString("")

  fun drawRect(g2d: Graphics2D, fbm: AceCanvas.FontBasedMeasurements, colors: Pair<Color, Color>) {
    val text = renderTag()
    val original = tagPoint
    val backgroundColor = yellow//if (text[0] == ' ') Color.YELLOW else colors.first
    val foregroundColor = yellow//if (text[0] == ' ') Color.YELLOW else colors.second

    original.translate(0, -fbm.hOffset.toInt())

    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    //a slight border for "pop" against the background
    g2d.color = backgroundColor

    if (text.length == 2) {
      g2d.drawRect(original.x - fbm.rectMarginWidth - 1,
        original.y - fbm.rectHOffset.toInt() - 1,
        fbm.rectWidth + fbm.fontWidth + 5, fbm.lineHeight.toInt() + 1)
    } else {
      g2d.drawRect(original.x - fbm.rectMarginWidth - 1,
        original.y - fbm.rectHOffset.toInt() - 1,
        fbm.rectWidth + 1, fbm.lineHeight.toInt() + 1)
    }

    //the background rectangle
    g2d.color = foregroundColor

    if (text.length == 2) {
      g2d.fillRect(original.x - fbm.rectMarginWidth,
        original.y - fbm.rectHOffset.toInt(),
        fbm.rectWidth + fbm.fontWidth + 5, fbm.lineHeight.toInt())
    } else {
      g2d.fillRect(original.x - fbm.rectMarginWidth,
        original.y - fbm.rectHOffset.toInt(),
        fbm.rectWidth, fbm.lineHeight.toInt())
    }

    //just a touch of alpha
    g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
      if (text[0] == ' ') 0.25.toFloat() else 0.85.toFloat())

    //the foreground text
    g2d.font = fbm.font
    g2d.color = BLACK
    g2d.drawString(text.toUpperCase(), original.x, original.y + fbm.fontHeight)
  }
}
