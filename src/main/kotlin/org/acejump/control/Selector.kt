package org.acejump.control

import org.acejump.search.Finder
import org.acejump.search.Jumper
import org.acejump.view.Model.caretOffset

object Selector {
  fun select(forward: Boolean = true) {
    val matches = nearestVisibleMatches(forward)
    if (matches.isEmpty()) return
    Jumper.jumpTo(matches.first())
    if (matches.size == 1) Handler.reset()
  }

  fun nearestVisibleMatches(forward: Boolean = true) =
    Finder.visibleResults().sortedWith(compareBy(
      { it == caretOffset },
      { (it <= caretOffset) == forward },
      { if (forward) it - caretOffset else caretOffset - it }
    ))
}