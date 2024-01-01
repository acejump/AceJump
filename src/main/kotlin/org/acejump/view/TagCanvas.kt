package org.acejump.view

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries.VISIBLE_ON_SCREEN
import org.acejump.read
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.SwingUtilities

/**
 * Holds all active tag markers and renders them on top of the editor.
 */
internal class TagCanvas(private val editor: Editor): JComponent(), CaretListener {
  private var markers: Collection<TagMarker>? = null
  init {
    val contentComponent = editor.contentComponent
    contentComponent.add(this)
    setBounds(0, 0, contentComponent.width, contentComponent.height)

    if (ApplicationInfo.getInstance().build.components.first() < 173)
      SwingUtilities.convertPoint(this, location, editor.component.rootPane)
        .let { setLocation(-it.x, -it.y) }

    editor.caretModel.addCaretListener(this)
  }

  fun unbind() {
    markers = null
    editor.contentComponent.remove(this)
    editor.caretModel.removeCaretListener(this)
  }

  /**
   * Ensures that all tags and the outline around the selected tag are
   * repainted. It should not be necessary to repaint the entire tag
   * canvas, but the cost of repainting visible tags is negligible.
   */
  override fun caretPositionChanged(event: CaretEvent) = repaint()

  fun setMarkers(markers: Collection<TagMarker>) {
    this.markers = markers
    repaint()
  }
  
  fun removeMarkers() {
    markers = emptyList()
  }

  override fun paint(g: Graphics) =
    read { if (!markers.isNullOrEmpty()) super.paint(g) else Unit }

  override fun paintChildren(g: Graphics) {
    super.paintChildren(g)

    val markers = markers ?: return
    
    (g as Graphics2D).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    
    val font = TagFont(editor)

    val cache = EditorOffsetCache.new()
    val viewRange = VISIBLE_ON_SCREEN.getOffsetRange(editor, cache)
    val foldingModel = editor.foldingModel
    val occupied = mutableListOf<Rectangle>()

    // If there is a tag at the caret location, prioritize its rendering over
    // all other tags. This is helpful for seeing which tag is currently
    // selected while navigating highly clustered tags, although it does end
    // up rearranging nearby tags which can be confusing.

    // TODO: instead of immediately painting, we could calculate the layout
    //  of everything first, and then remove tags that interfere with
    //  the caret tag to avoid changing the alignment of the caret tag
    
    val caretOffset = editor.caretModel.offset
    val caretMarker = markers.find { it.offsetL == caretOffset || it.offsetR == caretOffset }
    caretMarker?.paint(g, editor, cache, font, occupied)

    for (marker in markers) {
      if (marker.isOffsetInRange(viewRange) && !foldingModel.isOffsetCollapsed(marker.offsetL) && marker !== caretMarker)
        marker.paint(g, editor, cache, font, occupied)
    }
  }
}
