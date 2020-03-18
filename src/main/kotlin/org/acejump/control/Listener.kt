package org.acejump.control

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import org.acejump.control.Handler.redoFind
import org.acejump.label.Tagger
import org.acejump.search.getView
import org.acejump.view.Model.editor
import org.acejump.view.Model.viewBounds
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import kotlin.system.measureTimeMillis

internal object Listener: FocusListener, AncestorListener, VisibleAreaListener {
  private val logger = Logger.getInstance(Listener::class.java)

  fun enable() =
  // TODO: Do we really need `synchronized` here?
    synchronized(this) {
      editor.run {
        component.addFocusListener(Listener)
        component.addAncestorListener(Listener)
        scrollingModel.addVisibleAreaListener(Listener)
      }
    }

  fun disable() =
  // TODO: Do we really need `synchronized` here?
    synchronized(this) {
      editor.run {
        component.removeFocusListener(Listener)
        component.removeAncestorListener(Listener)
        scrollingModel.removeVisibleAreaListener(Listener)
      }
    }

  /**
   * This callback is very jittery. We need to delay repainting tags by a short
   * duration [Trigger] in order to prevent flashing tag syndrome.
   */

  override fun visibleAreaChanged(e: VisibleAreaEvent) {
    var elapsed = measureTimeMillis {
      if (canTagsSurviveViewResize()) {
        viewBounds = editor.getView()
        return
      }
    }
    elapsed = (750L - elapsed).coerceAtLeast(0L)
    Trigger(withDelay = elapsed) {
      logger.info("Visible area changed")
      redoFind()
    }
  }

  private fun canTagsSurviveViewResize() =
    editor.getView().run {
      if (first in viewBounds && last in viewBounds) return true
      else if (Tagger.full) return true
      else if (Tagger.regex) return false
      else !Tagger.hasMatchBetweenOldAndNewView(viewBounds, this)
    }

  override fun ancestorMoved(ancestorEvent: AncestorEvent?) {
    if (!canTagsSurviveViewResize()) {
      logger.info("Ancestor moved: $ancestorEvent")
      Handler.reset()
    }
  }

  override fun ancestorAdded(ancestorEvent: AncestorEvent?) =
    logger.info("Ancestor added: $ancestorEvent").also { Handler.reset() }

  override fun ancestorRemoved(ancestorEvent: AncestorEvent?) =
    logger.info("Ancestor removed: $ancestorEvent").also { Handler.reset() }

  override fun focusLost(focusEvent: FocusEvent?) =
    logger.info("Focus lost: $focusEvent").also { Handler.reset() }

  override fun focusGained(focusEvent: FocusEvent?) =
    logger.info("Focus gained: $focusEvent").also { Handler.reset() }
}