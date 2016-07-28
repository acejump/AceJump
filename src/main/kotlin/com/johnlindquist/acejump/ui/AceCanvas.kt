package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.ui.awt.RelativePoint
import com.johnlindquist.acejump.getPointFromVisualPosition
import java.awt.*
import java.util.*
import javax.swing.JComponent

class AceCanvas(val editor: EditorImpl) : JComponent() {
    var jumpInfos: MutableList<Pair<String, Point>> = arrayListOf()
    var colorPair = Pair<Color?, Color?>(Color.BLACK, Color.WHITE)
    var lineSpacing = 0.toFloat()
    var lineHeight = 0

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
        if (jumpInfos.isEmpty())
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
        jumpInfos.removeAll { true }
        repaint()
    }

    fun setupJumpLocations(results: Map<Int, String>) {
        if (results.size == 0)
            return //todo: hack, in case random keystrokes make it through

        val textPointPairs: MutableList<Pair<String, Point>> = ArrayList()
        val total = results.size - 1

        val letters = "abcdefghijklmnopqrstuvwxyz"
        val len = letters.length
        val groups = Math.floor(total.toDouble() / len)
        //            print("groups: " + groups.toString())
        val lenMinusGroups = len - groups.toInt()
        //            print("last letter: " + letters.charAt(lenMinusGroups).toString() + "\n")

        var i = 0
        for (textOffset in results.keys) {
            var str = ""

            val iGroup = i - lenMinusGroups
            val iModGroup = iGroup % len
            //                if(iModGroup == 0) print("================\n")
            val i1 = Math.floor(lenMinusGroups.toDouble() + ((i + groups.toInt()) / len)).toInt() - 1
            if (i >= lenMinusGroups) {
                str += letters.elementAt(i1)
                str += letters.elementAt(iModGroup).toString()
            } else {
                str += letters.elementAt(i).toString()
            }
            //                print(i.toString() + ": " + str + "     iModGroup:" + iModGroup.toString() + "\n")

            val point: RelativePoint? = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset))
            textPointPairs.add(Pair(str, point?.originalPoint as Point))

            if (str == "zz") {
                break
            }
            i++
        }
        showJumpers(textPointPairs)
    }

    fun showJumpers(textPointPairs: List<Pair<String, Point>>) {
        textPointPairs.reversed()
        repaint()
    }
}