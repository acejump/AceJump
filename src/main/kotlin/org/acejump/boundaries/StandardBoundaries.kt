package org.acejump.boundaries

import com.intellij.openapi.editor.Editor

enum class StandardBoundaries : Boundaries {
  WHOLE_FILE {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache) =
      0..editor.document.textLength
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache) =
      offset in (0..editor.document.textLength)
  },
  
  VISIBLE_ON_SCREEN {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
      val (topLeft, bottomRight) = cache.visibleArea(editor)
      val startOffset = cache.xyToOffset(editor, topLeft)
      val endOffset = cache.xyToOffset(editor, bottomRight)
      
      return startOffset..endOffset
    }
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean {
      return cache.isVisible(editor, offset)
    }
  },
  
  BEFORE_CARET {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache) =
      0..(editor.caretModel.offset)
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean =
      offset <= editor.caretModel.offset
  },
  
  AFTER_CARET {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache) =
      editor.caretModel.offset until editor.document.textLength
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean =
      offset >= editor.caretModel.offset
  }
}
