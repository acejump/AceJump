package org.acejump.search

import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import org.acejump.config.AceConfig
import org.acejump.view.Canvas
import org.acejump.view.Model.editor
import java.awt.Color

internal enum class JumpMode {
  DEFAULT, TARGET, DEFINE, WORD, LINE;

  companion object : Resettable {
    private var mode: JumpMode = DEFAULT
      set(value) {
        field = value
        when (field) {
          DEFINE -> setCaretColor(AceConfig.settings.definitionModeColor)
          TARGET -> setCaretColor(AceConfig.settings.targetModeColor)
          else -> setCaretColor(AceConfig.settings.jumpModeColor)
        }

        Finder.paintTextHighlights()
        Canvas.repaint()
      }

    private fun setCaretColor(color: Color) =
      editor.colorsScheme.setColor(CARET_COLOR, color)

    fun toggle(newMode: JumpMode? = null): JumpMode {
      mode = if (mode == newMode) DEFAULT
      else newMode ?: when (mode) {
        DEFAULT -> TARGET
        TARGET -> DEFINE
        else -> DEFAULT
      }

      return mode
    }

    override fun reset() {
      mode = DEFAULT
    }

    override fun equals(other: Any?) =
      if (other is JumpMode) mode == other else super.equals(other)
  }
}
