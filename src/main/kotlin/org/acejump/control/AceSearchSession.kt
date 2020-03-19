package org.acejump.control

import com.intellij.find.EditorSearchSession
import com.intellij.find.impl.livePreview.SearchResults
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.Editor
import org.acejump.search.AceFindModel

class AceSearchSession(editor: Editor, findModel: AceFindModel):
  EditorSearchSession(editor, editor.project, findModel) {
  init {
    editor.headerComponent = component
  }

  override fun searchResultsUpdated(sr: SearchResults) {
    super.searchResultsUpdated(sr)
  }

  override fun createPrimarySearchActions(): Array<AnAction> {
    return super.createPrimarySearchActions()
  }
}