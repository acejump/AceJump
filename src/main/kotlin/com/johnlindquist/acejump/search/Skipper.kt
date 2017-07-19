package com.johnlindquist.acejump.search

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType.CENTER
import com.johnlindquist.acejump.search.Finder.sitesToCheck
import com.johnlindquist.acejump.view.Model.editor

object Skipper {
  fun ifQueryExistsSkipToNextInEditor(isNext: Boolean) {
    val position = if (isNext) findNextPosition() ?: return
    else findPreviousPosition() ?: return
    editor.scrollingModel.scrollTo(position, CENTER)
  }

  private fun findPreviousPosition(): LogicalPosition? {
    val prevIndex = sitesToCheck
      .dropLastWhile { it < editor.getView().first }
      .lastOrNull() ?: sitesToCheck.lastOrNull() ?: return null

    val prevLogicalPosition = editor.offsetToLogicalPosition(prevIndex)

    // Try to capture as many previous results as will fit in a screenful
    fun maximizeCoverageOfPreviousOccurrence(): LogicalPosition {
      val minVisibleLine = prevLogicalPosition.line - editor.getScreenHeight()
      val firstVisibleIndex = editor.getLineStartOffset(minVisibleLine)
      val firstIndex = sitesToCheck.dropWhile { it < firstVisibleIndex }.first()
      val center = (prevIndex + firstIndex) / 2
      return editor.offsetToLogicalPosition(center)
    }

    return maximizeCoverageOfPreviousOccurrence()
  }

  private fun findNextPosition(): LogicalPosition? {
    val nextIndex = sitesToCheck
      .dropWhile { it < editor.getView().endInclusive }
      .firstOrNull() ?: sitesToCheck.firstOrNull() ?: return null

    val nextLogicalPosition = editor.offsetToLogicalPosition(nextIndex)

    // Try to capture as many subsequent results as will fit in a screenful
    fun maximizeCoverageOfNextOccurrence(): LogicalPosition {
      val maxVisibleLine = nextLogicalPosition.line + editor.getScreenHeight()
      val lastVisibleIndex = editor.getLineEndOffset(maxVisibleLine, true)
      val lastIndex = sitesToCheck.dropLastWhile { it > lastVisibleIndex }.last()
      val center = (nextIndex + lastIndex) / 2
      return editor.offsetToLogicalPosition(center)
    }

    return maximizeCoverageOfNextOccurrence()
  }
}
