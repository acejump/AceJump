package org.acejump.view

import org.acejump.search.getLineEndOffset
import org.acejump.search.getLineStartOffset
import org.acejump.view.Model.editor
import org.acejump.view.Model.editorText
import org.acejump.view.Model.viewBounds
import kotlin.math.max
import kotlin.math.min

/**
 * Interface which defines the boundary inside the file to be searched
 */

enum class Boundary: ClosedRange<Int> {
  // Search the complete file
  FULL_FILE_BOUNDARY {
    override val start: Int
      get() = 0
    override val endInclusive: Int
      get() = editorText.length
  },
  // Search only on the screen
  SCREEN_BOUNDARY {
    override val start: Int
      get() = Model.viewBounds.first
    override val endInclusive: Int
      get() = Model.viewBounds.last
  },
  // Search from the start of the screen to the caret
  BEFORE_CARET_BOUNDARY {
    override val start: Int
      get() = max(0, viewBounds.first)
    override val endInclusive: Int
      get() = min(editor.caretModel.offset, viewBounds.last)
  },
  // Search from the caret to the end of the screen
  AFTER_CARET_BOUNDARY {
    override val start: Int
      get() = max(editor.caretModel.offset, viewBounds.first)
    override val endInclusive: Int
      get() = viewBounds.last
  },
  // Search on the current line
  CURRENT_LINE_BOUNDARY {
    override val start: Int
      get() = editor.getLineStartOffset(editor.caretModel.logicalPosition.line)
    override val endInclusive: Int
      get() = editor.getLineEndOffset(editor.caretModel.logicalPosition.line)
  },
  // Search after caret within line
  CURRENT_LINE_AFTER_CARET_BOUNDARY {
    override val start: Int
    get() = max(editor.caretModel.offset, viewBounds.first)
    override val endInclusive: Int
    get() = editor.getLineEndOffset(editor.caretModel.logicalPosition.line)
  },
  // Search before caret within line
  CURRENT_LINE_BEFORE_CARET_BOUNDARY {
    override val start: Int
    get() = editor.getLineStartOffset(editor.caretModel.logicalPosition.line)
    override val endInclusive: Int
    get() = min(editor.caretModel.offset, viewBounds.last)
  };

  fun intRange() = IntRange(start, endInclusive)

  override fun toString() = super<ClosedRange>.toString() + " (${intRange()}) "
}