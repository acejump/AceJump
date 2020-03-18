package org.acejump.control

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType.CENTER
import com.intellij.openapi.editor.ScrollType.MAKE_VISIBLE
import org.acejump.label.Tagger.textMatches
import org.acejump.search.*
import org.acejump.view.Model.editor
import org.acejump.view.Model.viewBounds

/**
 * Updates the editor's vertical scroll position to make search results visible.
 * This will occur when the user presses TAB OR searches for text that does not
 * currently appear on the screen. Once scrolling is complete, the [Listener]
 * will [Trigger] an update that re-paint tags to the screen.
 */

object Scroller {
  private val logger = Logger.getInstance(Scroller::class.java)
  private var scrollX = 0
  private var scrollY = 0

  fun scroll(isNext: Boolean = true): Boolean {
    val position = if (isNext) findNextPosition() ?: return false
    else findPreviousPosition() ?: return false
    editor.scrollingModel.disableAnimation()
    editor.scrollingModel.scrollTo(position, CENTER)

    val firstInView = textMatches.first { it in editor.getView() }
    val horizontalOffset = editor.offsetToLogicalPosition(firstInView).column
    if(horizontalOffset > editor.scrollingModel.visibleArea.width)
      editor.scrollingModel.scrollHorizontally(horizontalOffset)

    viewBounds = editor.getView()

    return true
  }

  fun ensureCaretVisible(): Boolean {
    val initialArea = editor.scrollingModel.visibleArea
    editor.scrollingModel.scrollToCaret(MAKE_VISIBLE)
    return editor.scrollingModel.visibleArea == initialArea
  }

  private fun findPreviousPosition(): LogicalPosition? {
    val prevIndex = textMatches.toList().dropLastWhile { it > viewBounds.first }
      .lastOrNull() ?: textMatches.lastOrNull() ?: return null

    val prevLineNum = editor.offsetToLogicalPosition(prevIndex).line

    // Try to capture as many previous results as will fit in a screenful
    fun maximizeCoverageOfPreviousOccurrence(): LogicalPosition {
      val minVisibleLine = prevLineNum - editor.getScreenHeight()
      val firstVisibleIndex = editor.getLineStartOffset(minVisibleLine)
      val firstIndex = textMatches.dropWhile { it < firstVisibleIndex }.first()
      return editor.offsetCenter(firstIndex, prevIndex)
    }

    return maximizeCoverageOfPreviousOccurrence()
  }

  /**
   * Returns the center of the next set of results that will fit in the editor.
   * [textMatches] must be sorted prior to using Scroller. If [textMatches] have
   * not previously been sorted, the result of calling this method is undefined.
   */

  private fun findNextPosition(): LogicalPosition? {
    val nextIndex = textMatches.dropWhile { it <= viewBounds.last }
      .firstOrNull() ?: textMatches.firstOrNull() ?: return null

    val nextLineNum = editor.offsetToLogicalPosition(nextIndex).line

    // Try to capture as many subsequent results as will fit in a screenful
    fun maximizeCoverageOfNextOccurrence(): LogicalPosition {
      val maxVisibleLine = nextLineNum + editor.getScreenHeight()
      val lastVisibleIndex = editor.getLineEndOffset(maxVisibleLine, true)
      val lastIndex = textMatches.toList().dropLastWhile { it > lastVisibleIndex }.last()
      return editor.offsetCenter(nextIndex, lastIndex)
    }

    return maximizeCoverageOfNextOccurrence()
  }

  fun Editor.saveScroll() {
    scrollX = scrollingModel.horizontalScrollOffset
    scrollY = scrollingModel.verticalScrollOffset
  }

  fun Editor.restoreScroll() {
    if (caretModel.offset !in getView()) {
      scrollingModel.scrollVertically(scrollY)
      scrollingModel.scrollHorizontally(scrollX)
    }
  }
}
