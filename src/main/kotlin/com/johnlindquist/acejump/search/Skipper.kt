package com.johnlindquist.acejump.search

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType.*
import com.johnlindquist.acejump.KeyboardHandler
import com.johnlindquist.acejump.ui.AceUI.editor
import com.johnlindquist.acejump.ui.AceUI.editorText

object Skipper {
  fun ifQueryExistsSkipToNextInEditor(query: String) {
    KeyboardHandler.redoQueryImmediately
    editor.scrollingModel.scrollTo(findNextPosition(query), CENTER)
  }

  private fun findNextPosition(query: String): LogicalPosition {
//    Finder.sitesToCheck.dropWhile { it < editor.getView().endInclusive }.first()
    var nextIndex = editorText.indexOf(query, editor.getView().endInclusive)
    if (nextIndex < 0) nextIndex = editorText.indexOf(query, 0)
    val logicalPosition = editor.offsetToLogicalPosition(nextIndex)

    fun maximizeCoverageOfNextOccurence(): LogicalPosition {
      val maxVisibleLine = logicalPosition.line + editor.getScreenHeight()
      val lastPossibleIndex = editor.getLineEndOffset(maxVisibleLine, true)
      val toSearch = editorText.substring(nextIndex + query.length, lastPossibleIndex)
      val lastIndex = toSearch.lastIndexOf(query) // Last index in view
      val center = if (lastIndex > 0) nextIndex + lastIndex / 2 else nextIndex
      return editor.offsetToLogicalPosition(center)
    }

    return maximizeCoverageOfNextOccurence()
  }
}
