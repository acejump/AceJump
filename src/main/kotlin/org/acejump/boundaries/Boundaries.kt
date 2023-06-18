package org.acejump.boundaries

import com.intellij.openapi.editor.Editor
import kotlin.math.max
import kotlin.math.min

/**
 * Defines a (possibly) disjoint set of editor offsets that partitions
 * the whole editor into two groups - offsets inside the range, and
 * offsets outside the range.
 */
interface Boundaries {
  /**
   * Returns a range of editor offsets, starting at the first offset in the
   * boundary, and ending at the last offset in the boundary. May include
   * offsets outside the boundary, for ex. when the boundary is rectangular
   * and the file has long lines which are only partially visible.
   */
  fun getOffsetRange(editor: Editor, cache: EditorOffsetCache = EditorOffsetCache.Uncached): IntRange =
    StandardBoundaries.VISIBLE_ON_SCREEN.getOffsetRange(editor, cache)

  /**
   * Returns whether the editor offset is included within the boundary.
   */
  fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache = EditorOffsetCache.Uncached): Boolean =
    StandardBoundaries.VISIBLE_ON_SCREEN.isOffsetInside(editor, offset, cache)

  /**
   * Creates a boundary so that an offset/range is within the boundary
   * iff it is within both original boundaries.
   */
  fun intersection(other: Boundaries): Boundaries =
    if (this === other) this
    else object: Boundaries {
      override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
        val b1 = this@Boundaries.getOffsetRange(editor, cache)
        val b2 = other.getOffsetRange(editor, cache)
        return max(b1.first, b2.first)..min(b1.last, b2.last)
      }

      override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean =
        this@Boundaries.isOffsetInside(editor, offset, cache) && other.isOffsetInside(editor, offset, cache)
    }
}
