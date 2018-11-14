package org.acejump.view

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import org.acejump.search.*
import org.acejump.view.Model.fontWidth
import org.acejump.view.Model.viewBounds
import java.awt.*
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
      runLater { repaint() }
    }

  fun Editor.bindCanvas() {
    reset()
    storeBounds()
    contentComponent.add(Canvas)
    Canvas.setBounds(0, 0, contentComponent.width, contentComponent.height)

    if (ApplicationInfo.getInstance().build.components.first() < 173) {
      val loc = convertPoint(Canvas, location, component.rootPane)
      Canvas.setLocation(-loc.x, -loc.y)
    }
  }

  private fun Editor.storeBounds() {
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