package com.johnlindquist.acejump.control

import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.VisibleAreaEvent
import com.intellij.openapi.editor.event.VisibleAreaListener
import com.johnlindquist.acejump.control.Handler.redoFind
import com.johnlindquist.acejump.control.Handler.reset
import com.johnlindquist.acejump.search.Tagger
import com.johnlindquist.acejump.search.getView
import com.johnlindquist.acejump.view.Model.editor
import com.johnlindquist.acejump.view.Model.viewBounds
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import kotlin.system.measureTimeMillis

internal object Listener : CaretListener, FocusListener, AncestorListener,
  EditorColorsListener, VisibleAreaListener {
  fun enable() =
    synchronized(this) {
      editor.run {
        component.addFocusListener(Listener)
        component.addAncestorListener(Listener)
        scrollingModel.addVisibleAreaListener(Listener)
        caretModel.addCaretListener(Listener)
      }
    }

  fun disable() =
    synchronized(this) {
      editor.run {
        component.removeFocusListener(Listener)
        component.removeAncestorListener(Listener)
        scrollingModel.removeVisibleAreaListener(Listener)
        caretModel.removeCaretListener(Listener)
      }
    }

  override fun visibleAreaChanged(e: VisibleAreaEvent?) {
    val elapsed = measureTimeMillis { if (canTagsSurviveViewResize()) return }
    Trigger(withDelay = (750L - elapsed).coerceAtLeast(0L)) { redoFind() }
  }

  private fun canTagsSurviveViewResize() =
    editor.getView().run {
      if (first in viewBounds && last in viewBounds) return true
      else if (Tagger.isRegex) return false
      else !Tagger.hasMatchBetweenOldAndNewView(viewBounds, this)
    }

  override fun globalSchemeChange(scheme: EditorColorsScheme?) = redoFind()

  override fun ancestorAdded(event: AncestorEvent?) = reset()

  override fun ancestorMoved(event: AncestorEvent?) =
    if (canTagsSurviveViewResize()) Unit else reset()

  override fun ancestorRemoved(event: AncestorEvent?) = reset()

  override fun focusLost(e: FocusEvent?) = reset()

  override fun focusGained(e: FocusEvent?) = reset()

  override fun caretAdded(e: CaretEvent?) = reset()

  override fun caretPositionChanged(e: CaretEvent?) = reset()

  override fun caretRemoved(e: CaretEvent?) = reset()
}
