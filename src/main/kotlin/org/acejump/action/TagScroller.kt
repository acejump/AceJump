package org.acejump.action

import com.intellij.openapi.editor.*
import org.acejump.*
import org.acejump.search.SearchProcessor

internal class TagScroller(private val editor: Editor, private val searchProcessor: SearchProcessor) {
  fun scroll(
    forward: Boolean = true,
    position: LogicalPosition? = if (forward) findNextPosition() else findPreviousPosition()
  ) = if (position != null) true.also { scrollTo(position) } else false

  private fun scrollTo(position: LogicalPosition) = editor.run {
    scrollingModel.disableAnimation()
    scrollingModel.scrollTo(position, ScrollType.CENTER)

    val firstInView = textMatches.first { it in editor.getView() }
    val horizontalOffset = offsetToLogicalPosition(firstInView).column
    if (horizontalOffset > scrollingModel.visibleArea.width)
      scrollingModel.scrollHorizontally(horizontalOffset)
  }

  val textMatches by lazy { searchProcessor.results[editor]!! }

  private fun findPreviousPosition(): LogicalPosition? {
    val prevIndex = textMatches.toList().dropLastWhile { it > editor.getView().first }
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
    val nextIndex = textMatches.dropWhile { it <= editor.getView().last }
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
}
