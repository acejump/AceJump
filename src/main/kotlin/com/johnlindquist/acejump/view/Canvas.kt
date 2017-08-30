package com.johnlindquist.acejump.view

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.johnlindquist.acejump.view.Model.fontWidth
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import javax.swing.JComponent
import javax.swing.SwingUtilities.convertPoint
import kotlin.system.measureTimeMillis

/**
 * Overlay composed of all graphical tags. Maintains a registry of tags' visual
 * positions once assigned. We should avoid painting two tags to the same space.
 *
 * @see Marker
 */

object Canvas : JComponent() {
  private val logger = Logger.getInstance(Canvas::class.java)
  private val tags = hashSetOf<Point>()
  var jumpLocations: Collection<Marker> = emptyList()
    set(value) {
      field = value
      repaint()
    }

  fun Editor.bindCanvas() {
    contentComponent.add(Canvas)
    val viewport = scrollingModel.visibleArea
    Canvas.setBounds(0, 0, viewport.width + 1000, viewport.height + 1000)
    val loc = convertPoint(Canvas, location, component.rootPane)
    Canvas.setLocation(-loc.x, -loc.y)
  }

  override fun paint(graphics: Graphics) {
    if (jumpLocations.isEmpty()) return

    super.paint(graphics)
    tags.clear()
    jumpLocations.forEach { it.paintMe(graphics as Graphics2D) }
  }

  fun registerTag(pt: Point, tag: String) =
    (-1..tag.length).forEach { tags.add(Point(pt.x + it * fontWidth, pt.y)) }

  fun isFree(point: Point) = point !in tags

  fun reset() {
    jumpLocations = emptyList()
    tags.clear()
  }
}