package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.Editor
import com.johnlindquist.acejump.ui.AceUI.fontWidth
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import javax.swing.JComponent
import javax.swing.SwingUtilities.convertPoint

object Canvas : JComponent() {
  val existingTags = hashSetOf<Point>()
  var jumpLocations: Collection<JumpInfo> = emptyList<JumpInfo>()

  fun bindToEditor(editor: Editor) =
    editor.run {
      contentComponent.add(Canvas)
      val viewport = scrollingModel.visibleArea
      setBounds(0, 0, viewport.width + 1000, viewport.height + 1000)
      val loc = convertPoint(Canvas, location, component.rootPane)
      setLocation(-loc.x, -loc.y)
    }

  override fun paint(graphics: Graphics) {
    if (jumpLocations.isEmpty()) return

    super.paint(graphics)
    existingTags.clear()
    jumpLocations.forEach { it.paintMe(graphics as Graphics2D) }
  }

  fun registerTag(point: Point, tag: String) =
    (-1..tag.length).forEach {
      existingTags.add(Point(point.x + it * fontWidth, point.y))
    }

  fun isFree(point: Point) = !existingTags.contains(point)

  fun reset() {
    existingTags.clear()
    jumpLocations = emptyList()
  }
}