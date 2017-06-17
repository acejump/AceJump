package com.johnlindquist.acejump.search

import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.johnlindquist.acejump.ui.AceUI.editor

object Skipper {
  fun ifQueryExistsSkipToNextOccurenceInEditor(query: String) {
    editor.scrollingModel.scrollTo(findPosition(query), ScrollType.CENTER)
  }


  private fun findPosition(query: String): LogicalPosition = editor.offsetToLogicalPosition(0)

  fun maximizeCoverageOfNextOccurence() {}
}
