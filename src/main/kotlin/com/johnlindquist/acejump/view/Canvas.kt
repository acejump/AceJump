package com.johnlindquist.acejump.view

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.johnlindquist.acejump.search.Resettable
import com.johnlindquist.acejump.search.getView
import com.johnlindquist.acejump.view.Model.fontWidth
import com.johnlindquist.acejump.view.Model.viewBounds
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import javax.swing.JComponent
import javax.swing.SwingUtilities.convertPoint

/**
 * Overlay composed of all graphical tags. Maintains a registry of tags' visual
 * positions once assigned. We must avoid painting two tags to the same space.
 *
 * @see Marker
 */

object Canvas : JComponent(), Resettable {
  private val logger = Logger.getInstance(Canvas::class.java)
  private val occupied = hashSetOf<Point>()
  @Volatile
  var jumpLocations: Collection<Marker> = emptyList()
    set(value) {
      field = value
      repaint()
    }

  fun Editor.bindCanvas() {
    storeBounds()
    contentComponent.add(Canvas)
    Canvas.setBounds(0, 0, contentComponent.width, contentComponent.height)

    if(ApplicationInfo.getInstance().build.components.first() < 173) {
      val loc = convertPoint(Canvas, location, component.rootPane)
      Canvas.setLocation(-loc.x, -loc.y)
    }
  }

  fun Editor.storeBounds() {
    viewBounds = getView()
    this::offsetToLogicalPosition.let {
      logger.info("View bounds: $viewBounds (lines " +
        "${it(viewBounds.first).line}..${it(viewBounds.last).line})")
    }
  }

  override fun paint(graphics: Graphics) {
    if (jumpLocations.isEmpty()) return

    super.paint(graphics)
    occupied.clear()
    jumpLocations.forEach { it.paintMe(graphics as Graphics2D) }
  }

  fun registerTag(p: Point, tag: String) =
    (-1..tag.length).forEach { occupied.add(Point(p.x + it * fontWidth, p.y)) }

  fun isFree(point: Point) = point !in occupied

  override fun reset() {
    jumpLocations = emptyList()
    occupied.clear()
  }
}