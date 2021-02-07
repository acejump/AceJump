package org.acejump.view

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import org.acejump.search.*
import org.acejump.view.Model.fontWidth
import org.acejump.view.Model.viewBounds
import java.awt.Graphics
import java.awt.Point
import javax.swing.JComponent
import javax.swing.SwingUtilities.convertPoint

/**
 * Overlay composed of all tag [Marker]s. Maintains a registry of tags' visual
 * positions once assigned, so that we do not paint two tags to the same space.
 */

object Canvas: JComponent(), Resettable {
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
    setBounds(0, 0, contentComponent.width, contentComponent.height)

    if (ApplicationInfo.getInstance().build.components.first() < 173) {
      val loc = convertPoint(Canvas, location, component.rootPane)
      setLocation(-loc.x, -loc.y)
    }
  }

  private fun Editor.storeBounds() {
    viewBounds = getView()
    logger.info("View bounds: $viewBounds (lines " +
      "${offsetToLogicalPosition(viewBounds.first).line}.." +
      "${offsetToLogicalPosition(viewBounds.last).line})")
  }

  override fun paint(graphics: Graphics) {
    jumpLocations.ifEmpty { return }

    super.paint(graphics)
    occupied.clear()
    jumpLocations.forEach { it.paintMe(graphics) }
  }

  fun registerTag(p: Point, tag: String) =
    (-1..tag.length).forEach { occupied.add(Point(p.x + it * fontWidth, p.y)) }

  fun isFree(point: Point) = point !in occupied

  override fun reset() {
    jumpLocations = emptyList()
    occupied.clear()
  }
}