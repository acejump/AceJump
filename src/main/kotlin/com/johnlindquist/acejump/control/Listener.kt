package com.johnlindquist.acejump.control

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.johnlindquist.acejump.control.Handler.redoFind
import com.johnlindquist.acejump.control.Handler.reset
import com.johnlindquist.acejump.label.Tagger
import com.johnlindquist.acejump.search.getView
import com.johnlindquist.acejump.view.Model
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.viewBounds
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import kotlin.system.measureTimeMillis

internal object Listener : FocusListener, AncestorListener,
  EditorColorsListener, VisibleAreaListener {
  private val logger = Logger.getInstance(Listener::class.java)
  fun enable() =
    synchronized(this) {
      editor.run {
        component.addFocusListener(Listener)
        component.addAncestorListener(Listener)
        scrollingModel.addVisibleAreaListener(Listener)
      }
    }

  fun disable() =
    synchronized(this) {
      editor.run {
        component.removeFocusListener(Listener)
        component.removeAncestorListener(Listener)
        scrollingModel.removeVisibleAreaListener(Listener)
      }
    }

  /**
   * This callback is very jittery. We need to delay repainting tags by a short
   * duration in order to prevent flashing tag syndrome.
   *
   * @see Trigger
   */

  override fun visibleAreaChanged(e: VisibleAreaEvent?) {
    var elapsed = measureTimeMillis { if (canTagsSurviveViewResize()) return }
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

  override fun globalSchemeChange(scheme: EditorColorsScheme?) = redoFind()

  override fun ancestorAdded(ancestorEvent: AncestorEvent?) {
    logger.info("Ancestor added: $ancestorEvent")
    reset()
  }

  override fun ancestorMoved(ancestorEvent: AncestorEvent?) {
    if (!canTagsSurviveViewResize()) {
      logger.info("Ancestor moved: $ancestorEvent")
      reset()
    }
  }

  override fun ancestorRemoved(ancestorEvent: AncestorEvent?) {
    logger.info("Ancestor removed: $ancestorEvent")
    reset()
  }

  override fun focusLost(focusEvent: FocusEvent?) {
    logger.info("Focus lost: $focusEvent")
    reset()
  }

  override fun focusGained(focusEvent: FocusEvent?) {
    logger.info("Focus gained: $focusEvent")
    reset()
  }
}