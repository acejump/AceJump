package org.acejump.search

import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import org.acejump.config.AceConfig
import org.acejump.control.Handler
import org.acejump.label.Tagger
import org.acejump.view.Canvas
import org.acejump.view.Model
import org.acejump.view.Model.editor
import java.awt.Color

internal enum class JumpMode {
  DISABLED, DEFAULT, TARGET, DEFINE, WORD, LINE;

  companion object: Resettable {
    private var mode: JumpMode = DISABLED
      set(value) {
        field = value
        setCaretColor(when (field) {
          DEFAULT -> AceConfig.jumpModeColor
          DEFINE -> AceConfig.definitionModeColor
          TARGET -> AceConfig.targetModeColor
          DISABLED -> Model.naturalCaretColor
          else -> AceConfig.jumpModeColor
        })

        Finder.markup()
        Canvas.repaint()
      }

    private fun setCaretColor(color: Color) =
      editor.colorsScheme.setColor(CARET_COLOR, color)

    fun toggle(newMode: JumpMode? = null): JumpMode {
      mode = if (mode == newMode) DISABLED
      else newMode ?: when (mode) {
        DISABLED -> DEFAULT
        DEFAULT -> DEFINE
        DEFINE -> TARGET
        TARGET -> DISABLED.also { Handler.reset() }
        else -> DEFAULT
      }

      return mode
    }

    override fun reset() {
      mode = DISABLED
    }

    override fun equals(other: Any?) =
      if (other is JumpMode) mode == other else super.equals(other)
  }
}
