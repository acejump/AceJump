package org.acejump.search

import com.intellij.openapi.editor.Editor
import org.acejump.getView

data class Tag(val editor: Editor, val offset: Int) {
  fun isVisible() = offset in editor.getView() && !editor.foldingModel.isOffsetCollapsed(offset)
}
