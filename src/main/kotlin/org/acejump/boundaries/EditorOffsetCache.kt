package org.acejump.boundaries

import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import java.awt.Point

/**
 * Optionally caches slow operations of (1) retrieving the currently visible editor area, and (2) converting between editor offsets and
 * pixel coordinates.
 *
 * To avoid unnecessary overhead, there is no automatic detection of when the editor, its contents, or its visible area has changed, so the
 * cache must only be used for a single rendered frame of a single [Editor].
 */
sealed class EditorOffsetCache {
  /**
   * Returns the top left and bottom right points of the visible area rectangle.
   */
  abstract fun visibleArea(editor: Editor): Pair<Point, Point>
  
  /**
   * Returns the editor offset at the provided pixel coordinate.
   */
  abstract fun xyToOffset(editor: Editor, pos: Point): Int
  
  /**
   * Returns the top left pixel coordinate of the character at the provided editor offset.
   */
  abstract fun offsetToXY(editor: Editor, offset: Int): Point
  
  companion object {
    fun new(): EditorOffsetCache {
      return Cache()
    }
  }
  
  private class Cache : EditorOffsetCache() {
    private var visibleArea: Pair<Point, Point>? = null
    private val pointToOffset = Object2IntOpenHashMap<Point>().apply { defaultReturnValue(-1) }
    private val offsetToPoint = Int2ObjectOpenHashMap<Point>()
    
    override fun visibleArea(editor: Editor): Pair<Point, Point> {
      return visibleArea ?: Uncached.visibleArea(editor).also { visibleArea = it }
    }
    
    override fun xyToOffset(editor: Editor, pos: Point): Int {
      val offset = pointToOffset.getInt(pos)
      
      if (offset != -1) {
        return offset
      }
      
      return Uncached.xyToOffset(editor, pos).also {
        @Suppress("ReplacePutWithAssignment")
        pointToOffset.put(pos, it)
      }
    }
    
    override fun offsetToXY(editor: Editor, offset: Int): Point {
      val pos = offsetToPoint.get(offset)
      
      if (pos != null) {
        return pos
      }
      
      return Uncached.offsetToXY(editor, offset).also {
        @Suppress("ReplacePutWithAssignment")
        offsetToPoint.put(offset, it)
      }
    }
  }
  
  object Uncached : EditorOffsetCache() {
    override fun visibleArea(editor: Editor): Pair<Point, Point> {
      val visibleRect = editor.scrollingModel.visibleArea
      
      return Pair(
        visibleRect.location,
        visibleRect.location.apply { translate(visibleRect.width, visibleRect.height) }
      )
    }
    
    override fun xyToOffset(editor: Editor, pos: Point): Int {
      return editor.logicalPositionToOffset(editor.xyToLogicalPosition(pos))
    }
    
    override fun offsetToXY(editor: Editor, offset: Int): Point {
      return editor.offsetToXY(offset, true, false)
    }
  }
}
