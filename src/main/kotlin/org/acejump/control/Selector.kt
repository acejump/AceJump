package org.acejump.control

import org.acejump.search.Finder
import org.acejump.search.Jumper
import org.acejump.view.Model.caretOffset

/**
 * Supports cyclical selection of tags using the ENTER/SHIFT+ENTER keys.
 *
 * @see [Handler.editorActionMap]
 */

object Selector {
  fun select(forward: Boolean = true) {
    val matches = nearestVisibleMatches(forward)
    matches.ifEmpty { return }
    Jumper.jumpTo(matches.first(), false)
    val wasAlreadyVisible = Scroller.ensureCaretVisible()
    if (matches.size == 1 && wasAlreadyVisible) Handler.reset()
  }

  fun nearestVisibleMatches(forward: Boolean = true): List<Int> {
    val caretOffset = caretOffset

    return Finder.visibleResults().sortedWith(compareBy(
      { it == caretOffset },
      { (it <= caretOffset) == forward },
      { if (forward) it - caretOffset else caretOffset - it }
    ))
  }
}