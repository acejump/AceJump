package org.acejump.boundaries

import com.intellij.openapi.editor.Editor
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import java.awt.Point

/**
 * Optionally caches slow operations of (1) retrieving the currently
 * visible editor area, and (2) converting between editor offsets and
 * pixel coordinates.
 *
 * To avoid unnecessary overhead, there is no automatic detection of when
 * the editor, its contents, or its visible area has changed, so the cache
 * must only be used for a single rendered frame of a single [Editor].
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

  companion object { fun new(): EditorOffsetCache = Cache() }

  private class Cache: EditorOffsetCache() {
    private var visibleArea: Pair<Point, Point>? = null
    private val pointToOffset = Object2IntOpenHashMap<Point>().apply { defaultReturnValue(-1) }
    private val offsetToPoint = Int2ObjectOpenHashMap<Point>()

    override fun visibleArea(editor: Editor): Pair<Point, Point> =
      visibleArea ?: Uncached.visibleArea(editor).also { visibleArea = it }

    override fun xyToOffset(editor: Editor, pos: Point): Int =
      pointToOffset.getInt(pos).let { offset ->
        if (offset != -1) offset
        else Uncached.xyToOffset(editor, pos)
          .also { pointToOffset.put(pos, it) }
      }

    override fun offsetToXY(editor: Editor, offset: Int) =
      offsetToPoint.get(offset) ?: Uncached.offsetToXY(editor, offset)
        .also { offsetToPoint.put(offset, it) }
  }

  object Uncached: EditorOffsetCache() {
    override fun visibleArea(editor: Editor): Pair<Point, Point> =
      editor.scrollingModel.visibleArea.let { visibleRect ->
        Pair(
          visibleRect.location, visibleRect.location.apply {
            translate(visibleRect.width, visibleRect.height)
          }
        )
      }

    override fun xyToOffset(editor: Editor, pos: Point): Int =
      editor.logicalPositionToOffset(editor.xyToLogicalPosition(pos))

    override fun offsetToXY(editor: Editor, offset: Int): Point =
      editor.offsetToXY(offset, true, false)
  }
}
