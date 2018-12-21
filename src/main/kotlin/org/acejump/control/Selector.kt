package org.acejump.control

import org.acejump.search.Finder
import org.acejump.search.Jumper
import org.acejump.view.Model.caretOffset

object Selector {
  fun select(forward: Boolean = true) = Jumper.jumpTo(nearestVisible(forward))

  fun nearestVisible(forward: Boolean = true) =
    Finder.visibleResult().run { if (forward) nextItem() else previousItem() }

  private fun List<Int>.previousItem() =
    lastOrNull { it < caretOffset } ?: lastOrNull() ?: caretOffset

  private fun List<Int>.nextItem() =
    firstOrNull { it > caretOffset } ?: firstOrNull() ?: caretOffset
}