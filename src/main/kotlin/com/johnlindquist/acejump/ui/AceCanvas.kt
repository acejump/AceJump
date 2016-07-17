package com.johnlindquist.acejump.ui

import java.awt.*
import javax.swing.JComponent


class AceCanvas : JComponent() {
    var jumpInfos: List<Pair<String, Point>>? = null
    var colorPair = Pair<Color?, Color?>(Color.BLACK, Color.WHITE)
    var lineSpacing: Float = 0.toFloat()
    var lineHeight: Int = 0

    inner class FontBasedMeasurements() {
        var font = getFont()!!
        val fontWidth = getFontMetrics(font).stringWidth("w")
        val fontHeight = font.size

        val rectMarginWidth = fontWidth / 2
        val doubleRectMarginWidth = rectMarginWidth * 2

        val fontSpacing = fontHeight * lineSpacing
        val rectHOffset = fontSpacing - fontHeight
        val rectWidth = fontWidth + doubleRectMarginWidth

        val hOffset = fontHeight - fontSpacing
    }

    override fun paint(graphics: Graphics) {
        if (jumpInfos == null)
            return

        super.paint(graphics)

        val g2d = graphics as Graphics2D
        val fbm = FontBasedMeasurements()

        for (jumpInfo: Pair<String, Point> in jumpInfos.orEmpty()) {
            val text = jumpInfo.first
            val originalPoint = jumpInfo.second
            val defaultForeground = colorPair.second
            val defaultBackground = colorPair.first

            originalPoint.translate(0, -fbm.hOffset.toInt())

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            //a slight border for "pop" against the background
            g2d.color = defaultBackground

            if (text.length == 2) {
                g2d.drawRect(originalPoint.x - fbm.rectMarginWidth - 1, originalPoint.y - fbm.rectHOffset.toInt() - 1, fbm.rectWidth + fbm.fontWidth + 5, lineHeight.toInt() + 1)
            } else {
                g2d.drawRect(originalPoint.x - fbm.rectMarginWidth - 1, originalPoint.y - fbm.rectHOffset.toInt() - 1, fbm.rectWidth + 1, lineHeight.toInt() + 1)
            }

            //the background rectangle
            g2d.color = defaultForeground

            if (text.length == 2) {
                g2d.fillRect(originalPoint.x - fbm.rectMarginWidth, originalPoint.y - fbm.rectHOffset.toInt(), fbm.rectWidth + fbm.fontWidth + 5, lineHeight.toInt())
            } else {
                g2d.fillRect(originalPoint.x - fbm.rectMarginWidth, originalPoint.y - fbm.rectHOffset.toInt(), fbm.rectWidth, lineHeight.toInt())
            }



            //just a touch of alpha
            g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85.toFloat())

            //the foreground text
            g2d.font = fbm.font
            g2d.color = defaultBackground
            g2d.drawString(text.toUpperCase(), originalPoint.x, originalPoint.y + fbm.fontHeight)

        }
    }

    fun clear() {
        jumpInfos = null
        repaint()
    }
}