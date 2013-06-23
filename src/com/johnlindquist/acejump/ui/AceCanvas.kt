package com.johnlindquist.acejump.ui

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import javax.swing.JComponent
import java.awt.RenderingHints
import java.awt.AlphaComposite
import java.awt.Font


public class AceCanvas: JComponent() {
    var jumpInfos: List<Pair<String, Point>>? = null
        public set
    var colorPair = Pair<Color?, Color?>(Color.BLACK, Color.WHITE)
        public set
    var lineSpacing:Float = 0.toFloat()
        public set
    var lineHeight:Int = 0
        public set


    inner class FontBasedMeasurements() {
        var font = getFont()
        val fontWidth = getFontMetrics(font)?.stringWidth("w")!!
        val fontHeight = font?.getSize()!!

        val rectMarginWidth = fontWidth / 2
        val doubleRectMarginWidth = rectMarginWidth * 2

        val fontSpacing = fontHeight * lineSpacing
        val rectHOffset = fontSpacing - fontHeight
        val rectWidth = fontWidth + doubleRectMarginWidth

        val hOffset = fontHeight - fontSpacing
    }

    public override fun paint(p0: Graphics) {
        super<JComponent>.paint(p0)

        if(jumpInfos == null)
            return
        val g2d = p0 as Graphics2D
        val fbm = FontBasedMeasurements()


        for (jumpInfo in jumpInfos?.iterator()){


            val text = jumpInfo.first
            val originalPoint = jumpInfo.second
            val defaultForeground = colorPair.second
            val defaultBackground = colorPair.first

            originalPoint.translate(0, -fbm.hOffset.toInt())

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)


            //a slight border for "pop" against the background
            g2d.setColor(defaultBackground)

            if(text.length == 2){
                g2d.drawRect(originalPoint.x - fbm.rectMarginWidth - 1, originalPoint.y - fbm.rectHOffset.toInt() - 1, fbm.rectWidth + fbm.fontWidth + 1, lineHeight.toInt() + 1)
            }else{
                g2d.drawRect(originalPoint.x - fbm.rectMarginWidth - 1, originalPoint.y - fbm.rectHOffset.toInt() - 1, fbm.rectWidth + 1, lineHeight.toInt() + 1)
            }

            //the background rectangle
            g2d.setColor(defaultForeground)

            if(text.length == 2){
                g2d.fillRect(originalPoint.x - fbm.rectMarginWidth, originalPoint.y - fbm.rectHOffset.toInt(), fbm.rectWidth + fbm.fontWidth, lineHeight.toInt())
            }else{
                g2d.fillRect(originalPoint.x - fbm.rectMarginWidth, originalPoint.y - fbm.rectHOffset.toInt(), fbm.rectWidth, lineHeight.toInt())
            }



            //just a touch of alpha
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85.toFloat()))

            //the foreground text
            g2d.setFont(fbm.font)
            g2d.setColor(defaultBackground)
            g2d.drawString(text.toUpperCase(), originalPoint.x, originalPoint.y + fbm.fontHeight)

        }
    }

    public fun clear() {
        jumpInfos = null
        repaint()
    }
}