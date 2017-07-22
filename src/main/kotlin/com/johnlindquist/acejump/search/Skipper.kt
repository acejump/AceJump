package com.johnlindquist.acejump.search

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType.CENTER
import com.johnlindquist.acejump.search.Finder.textMatches
import com.johnlindquist.acejump.view.Model.editor

/**
 * Responsible for changing the vertical scroll position inside an editor.
 */

object Skipper {
  fun doesQueryExistIfSoSkipToIt(isNext: Boolean = true): Boolean {
    val position = if (isNext) findNextPosition() ?: return false
    else findPreviousPosition() ?: return false
    editor.scrollingModel.disableAnimation()
    editor.scrollingModel.scrollTo(position, CENTER)

    return true
  }

  private fun findPreviousPosition(): LogicalPosition? {
    val prevIndex = textMatches
      .dropLastWhile { it < editor.getView().first }
      .lastOrNull() ?: textMatches.lastOrNull() ?: return null

    val prevLogicalPosition = editor.offsetToLogicalPosition(prevIndex)

    // Try to capture as many previous results as will fit in a screenful
    fun maximizeCoverageOfPreviousOccurrence(): LogicalPosition {
      val minVisibleLine = prevLogicalPosition.line - editor.getScreenHeight()
      val firstVisibleIndex = editor.getLineStartOffset(minVisibleLine)
      val firstIndex = textMatches.dropWhile { it < firstVisibleIndex }.first()
      val center = (prevIndex + firstIndex) / 2
      return editor.offsetToLogicalPosition(center)
    }

    return maximizeCoverageOfPreviousOccurrence()
  }

  private fun findNextPosition(): LogicalPosition? {
    val nextIndex = textMatches
      .dropWhile { it < editor.getView().endInclusive }
      .firstOrNull() ?: textMatches.firstOrNull() ?: return null

    val nextLogicalPosition = editor.offsetToLogicalPosition(nextIndex)

    // Try to capture as many subsequent results as will fit in a screenful
    fun maximizeCoverageOfNextOccurrence(): LogicalPosition {
      val maxVisibleLine = nextLogicalPosition.line + editor.getScreenHeight()
      val lastVisibleIndex = editor.getLineEndOffset(maxVisibleLine, true)
      val lastIndex = textMatches.dropLastWhile { it > lastVisibleIndex }.last()
      val center = (nextIndex + lastIndex) / 2
      return editor.offsetToLogicalPosition(center)
    }

    return maximizeCoverageOfNextOccurrence()
  }
}
