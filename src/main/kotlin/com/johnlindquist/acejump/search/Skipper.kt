package com.johnlindquist.acejump.search

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType.*
import com.johnlindquist.acejump.KeyboardHandler
import com.johnlindquist.acejump.search.Finder.sitesToCheck
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.editorText

object Skipper {
  fun ifQueryExistsSkipToNextInEditor(isPrevious: Boolean = false) {
    val nextPosition = findNextPosition() ?: return
    editor.scrollingModel.scrollTo(nextPosition, CENTER)
  }

  fun findPreviousPosition(): LogicalPosition? {
    val prevIndex = sitesToCheck
      .dropLastWhile { it < editor.getView().first }
      .lastOrNull() ?: sitesToCheck.lastOrNull() ?: return null

    val prevLogicalPosition = editor.offsetToLogicalPosition(prevIndex)

    // Try to capture as many subsequent results as will fit in a screenful
    fun maximizeCoverageOfNextOccurence(): LogicalPosition {
      val minVisibleLine = prevLogicalPosition.line - editor.getScreenHeight()
      val firstVisibleIndex = editor.getLineEndOffset(minVisibleLine, true)
      val lastIndex = sitesToCheck.dropWhile { it < firstVisibleIndex }.first()
      val center = (prevIndex + lastIndex) / 2
      return editor.offsetToLogicalPosition(center)
    }

    return maximizeCoverageOfNextOccurence()
  }

  private fun findNextPosition(): LogicalPosition? {
    val nextIndex = sitesToCheck
      .dropWhile { it < editor.getView().endInclusive }
      .firstOrNull() ?: sitesToCheck.firstOrNull() ?: return null

    val nextLogicalPosition = editor.offsetToLogicalPosition(nextIndex)

    // Try to capture as many subsequent results as will fit in a screenful
    fun maximizeCoverageOfNextOccurence(): LogicalPosition {
      val maxVisibleLine = nextLogicalPosition.line + editor.getScreenHeight()
      val lastVisibleIndex = editor.getLineEndOffset(maxVisibleLine, true)
      val lastIndex = sitesToCheck.dropLastWhile { it > lastVisibleIndex }.last()
      val center = (nextIndex + lastIndex) / 2
      return editor.offsetToLogicalPosition(center)
    }

    return maximizeCoverageOfNextOccurence()
  }
}
