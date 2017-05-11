package com.johnlindquist.acejump.ui

import com.johnlindquist.acejump.ui.AceUI.fontWidth
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent

object Canvas : JComponent() {
  var existingTags = hashSetOf<Pair<Int, Int>>()
  var jumpLocations: Collection<JumpInfo> = arrayListOf()

  override fun paint(graphics: Graphics) {
    if (jumpLocations.isEmpty())
      return

    super.paint(graphics)
    val graphics2D = graphics as Graphics2D
    existingTags = hashSetOf<Pair<Int, Int>>()
    jumpLocations.forEach { it.paintMe(graphics2D) }
  }

  fun registerTag(point: Pair<Int, Int>, tag: String) =
    (-1..(tag.length)).forEach {
      existingTags.add(Pair(point.first + it * fontWidth, point.second))
    }

  fun isFree(point: Pair<Int, Int>) = !existingTags.contains(point)

  fun reset() {
    existingTags = hashSetOf<Pair<Int, Int>>()
    jumpLocations = arrayListOf()
  }
}