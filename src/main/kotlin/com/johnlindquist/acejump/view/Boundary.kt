package com.johnlindquist.acejump.view

import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.editorText
import com.johnlindquist.acejump.view.Model.viewBounds
import kotlin.math.max
import kotlin.math.min

/**
 * Interface which defines the boundary inside the file to be searched
 */
interface Boundary : ClosedRange<Int> {
  /**
   * Search the complete file
   */
  object FullFileBoundary : Boundary {
    override val start: Int
      get() = 0
    override val endInclusive: Int
      get() = editorText.length
  }

  /**
   * Search only on the screen
   */
  object ScreenBoundary : Boundary {
    override val start: Int
      get() = Model.viewBounds.first
    override val endInclusive: Int
      get() = Model.viewBounds.last
  }

  /**
   * Search from the start of the screen to the caret
   */
  object BeforeCaretBoundary : Boundary {
    override val start: Int
      get() = max(0, viewBounds.first)
    override val endInclusive: Int
      get() = min(editor.caretModel.offset, viewBounds.last)
  }

  /**
   * Search from the caret to the end of the screen
   */
  object AfterCaretBoundary : Boundary {
    override val start: Int
      get() = max(editor.caretModel.offset, viewBounds.first)
    override val endInclusive: Int
      get() = viewBounds.last
  }
}