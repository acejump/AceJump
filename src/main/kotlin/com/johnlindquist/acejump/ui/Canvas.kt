package com.johnlindquist.acejump.ui

import com.intellij.openapi.editor.Editor
import com.johnlindquist.acejump.ui.AceUI.fontWidth
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent
import javax.swing.SwingUtilities.convertPoint

object Canvas : JComponent() {
  var existingTags = hashSetOf<Pair<Int, Int>>()
  var jumpLocations: Collection<JumpInfo> = arrayListOf()

  fun bindToEditor(editor: Editor) =
    editor.run {
      contentComponent.add(Canvas)
      val viewport = scrollingModel.visibleArea
      setBounds(0, 0, viewport.width + 1000, viewport.height + 1000)
      val loc = convertPoint(Canvas, location, component.rootPane)
      setLocation(-loc.x, -loc.y)
    }

  override fun paint(graphics: Graphics) {
    if (jumpLocations.isEmpty())
      return

    super.paint(graphics)
    val graphics2D = graphics as Graphics2D
    existingTags = hashSetOf<Pair<Int, Int>>()
    jumpLocations.forEach { it.paintMe(graphics2D) }
  }

  fun registerTag(point: Pair<Int, Int>, tag: String) =
    (-1..tag.length).forEach {
      existingTags.add(Pair(point.first + it * fontWidth, point.second))
    }

  fun isFree(point: Pair<Int, Int>) = !existingTags.contains(point)

  fun reset() {
    existingTags = hashSetOf<Pair<Int, Int>>()
    jumpLocations = arrayListOf()
  }
}