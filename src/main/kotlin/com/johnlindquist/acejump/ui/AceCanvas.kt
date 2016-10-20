package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.impl.EditorImpl
import java.awt.Font
import java.awt.Font.BOLD
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent

class AceCanvas(val editor: EditorImpl) : JComponent() {
  var jumpInfos: List<JumpInfo> = arrayListOf()
  val scheme = EditorColorsManager.getInstance().globalScheme
  val colors = Pair(scheme.defaultBackground, scheme.defaultForeground)

  init {
    font = Font(scheme.editorFontName, BOLD, scheme.editorFontSize)
  }

  inner class FontBasedMeasurements() {
    var font = getFont()!!
    val fontWidth = getFontMetrics(font).stringWidth("w")
    val fontHeight = font.size
    val lineHeight = editor.lineHeight
    val lineSpacing = scheme.lineSpacing
    val rectMarginWidth = fontWidth / 2
    val doubleRectMarginWidth = rectMarginWidth * 2
    val fontSpacing = fontHeight * lineSpacing
    val rectHOffset = fontSpacing - fontHeight
    val rectWidth = doubleRectMarginWidth
    val hOffset = fontHeight - fontSpacing
  }

  override fun paint(graphics: Graphics) {
    if (jumpInfos.isEmpty())
      return

    super.paint(graphics)

    val g2d = graphics as Graphics2D
    val fbm = FontBasedMeasurements()
    jumpInfos.orEmpty().forEach { it.drawRect(g2d, fbm, colors) }
  }
}