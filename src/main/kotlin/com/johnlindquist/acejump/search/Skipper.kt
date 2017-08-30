package com.johnlindquist.acejump.search

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType.CENTER
import com.johnlindquist.acejump.label.Tagger.textMatches
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.viewBounds

/**
 * Updates the editor's vertical scroll position to make search results visible.
 * This will occur when the user presses TAB OR searches for text that does not
 * currently appear on the screen. Once scrolling is complete, the Listener will
 * trigger an update that re-paint tags to the screen.
 *
 * @see com.johnlindquist.acejump.control.Listener
 */

object Skipper {
  private val logger = Logger.getInstance(Skipper::class.java)
  private var scrollX = 0
  private var scrollY = 0

  fun ifQueryExistsSkipAhead(isNext: Boolean = true): Boolean {
    val position = if (isNext) findNextPosition() ?: return false
    else findPreviousPosition() ?: return false
    editor.scrollingModel.disableAnimation()
    editor.scrollingModel.scrollTo(position, CENTER)
    viewBounds = editor.getView()

    return true
  }

  private fun findPreviousPosition(): LogicalPosition? {
    val prevIndex = textMatches.toList().dropLastWhile { it < viewBounds.first }
      .lastOrNull() ?: textMatches.lastOrNull() ?: return null

    val prevLogicalPosition = editor.offsetToLogicalPosition(prevIndex)

    // Try to capture as many previous results as will fit in a screenful
    fun maximizeCoverageOfPreviousOccurrence(): LogicalPosition {
      val minVisibleLine = prevLogicalPosition.line - editor.getScreenHeight()
      val firstVisibleIndex = editor.getLineStartOffset(minVisibleLine)
      val firstIndex = textMatches.dropWhile { it < firstVisibleIndex }.first()
      return editor.offsetCenter(firstIndex, prevIndex)
    }

    return maximizeCoverageOfPreviousOccurrence()
  }

  private fun findNextPosition(): LogicalPosition? {
    val nextIndex = textMatches.dropWhile { it <= viewBounds.last }
      .firstOrNull() ?: textMatches.firstOrNull() ?: return null

    val nextLogicalPosition = editor.offsetToLogicalPosition(nextIndex)

    // Try to capture as many subsequent results as will fit in a screenful
    fun maximizeCoverageOfNextOccurrence(): LogicalPosition {
      val maxVisibleLine = nextLogicalPosition.line + editor.getScreenHeight()
      val lastVisibleIndex = editor.getLineEndOffset(maxVisibleLine, true)
      val lastIndex = textMatches.toList().dropLastWhile { it > lastVisibleIndex }.last()
      return editor.offsetCenter(nextIndex, lastIndex)
    }

    return maximizeCoverageOfNextOccurrence()
  }


  fun Editor.storeScroll() {
    scrollX = scrollingModel.horizontalScrollOffset
    scrollY = scrollingModel.verticalScrollOffset
  }

  fun Editor.restoreScroll() {
    if (caretModel.offset !in getView()) {
      scrollingModel.scrollVertically(scrollY)
      scrollingModel.scrollHorizontally(scrollX)
    }
  }

  fun Editor.storeBounds() {
    viewBounds = getView()
    this::offsetToLogicalPosition.let {
      logger.info("View bounds: $viewBounds (lines " +
        "${it(viewBounds.first).line}.." +
        "${it(viewBounds.last).line})")
    }
  }
}
