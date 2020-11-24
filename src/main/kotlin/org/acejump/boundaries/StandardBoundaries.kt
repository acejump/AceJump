package org.acejump.boundaries

import com.intellij.openapi.editor.Editor

enum class StandardBoundaries : Boundaries {
  WHOLE_FILE {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
      return 0 until editor.document.textLength
    }
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean {
      return offset in (0 until editor.document.textLength)
    }
  },
  
  VISIBLE_ON_SCREEN {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
      val (topLeft, bottomRight) = cache.visibleArea(editor)
      val startOffset = cache.xyToOffset(editor, topLeft)
      val endOffset = cache.xyToOffset(editor, bottomRight)
      
      return startOffset..endOffset
    }
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean {
      
      // If we are not using a cache, calling getOffsetRange will cause additional 1-2 pixel coordinate -> offset lookups, which is a lot
      // more expensive than one lookup compared against the visible area.
      
      // However, if we are using a cache, it's likely that the topmost and bottommost positions are already cached whereas the provided
      // offset isn't, so we save a lookup for every offset outside the range.
      
      if (cache !== EditorOffsetCache.Uncached && offset !in getOffsetRange(editor, cache)) {
        return false
      }
      
      val (topLeft, bottomRight) = cache.visibleArea(editor)
      val pos = cache.offsetToXY(editor, offset)
      val x = pos.x
      val y = pos.y
      
      return x >= topLeft.x && y >= topLeft.y && x <= bottomRight.x && y <= bottomRight.y
    }
  },
  
  BEFORE_CARET {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
      return 0..(editor.caretModel.offset)
    }
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean {
      return offset <= editor.caretModel.offset
    }
  },
  
  AFTER_CARET {
    override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
      return editor.caretModel.offset until editor.document.textLength
    }
    
    override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean {
      return offset >= editor.caretModel.offset
    }
  }
}
