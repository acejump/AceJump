package org.acejump.search

import com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR
import org.acejump.config.AceConfig
import org.acejump.control.Handler
import org.acejump.view.Canvas
import org.acejump.view.Model
import org.acejump.view.Model.editor
import java.awt.Color

enum class JumpMode {
  DISABLED, JUMP, TARGET, DEFINE;

  companion object: Resettable {
    private var modeIndex = 0
    private var mode: JumpMode = DISABLED
      set(value) {
        field = value
        setCaretColor(when (field) {
          JUMP -> AceConfig.jumpModeColor
          DEFINE -> AceConfig.definitionModeColor
          TARGET -> AceConfig.targetModeColor
          DISABLED -> Model.naturalCaretColor
        })

        Finder.markup()
        Canvas.repaint()
      }

    private fun setCaretColor(color: Color) =
      editor.colorsScheme.setColor(CARET_COLOR, color)

    fun toggle(newMode: JumpMode): JumpMode {
      if (mode == newMode) {
        mode = DISABLED
        modeIndex = 0
        Handler.reset()
      }
      else {
        mode = newMode
        modeIndex = cycleSettings.indexOfFirst { it == newMode } + 1
      }
      return mode
    }

    private val cycleSettings
      get() = arrayOf(
        AceConfig.cycleMode1,
        AceConfig.cycleMode2,
        AceConfig.cycleMode3,
        AceConfig.cycleMode4
      )

    fun cycle(): JumpMode {
      val cycleSettings = cycleSettings

      for (testModeIndex in (modeIndex + 1)..(cycleSettings.size)) {
        if (cycleSettings[testModeIndex - 1] != DISABLED) {
          mode = cycleSettings[testModeIndex - 1]
          modeIndex = testModeIndex
          return mode
        }
      }

      mode = DISABLED
      modeIndex = 0
      Handler.reset()
      return mode
    }

    override fun reset() {
      mode = DISABLED
      modeIndex = 0
    }

    override fun equals(other: Any?) =
      if (other is JumpMode) mode == other else super.equals(other)

  }

  override fun toString() = when(this) {
    DISABLED -> aceString("jumpModeDisabled")
    JUMP     -> aceString("jumpModeJump")
    TARGET   -> aceString("jumpModeTarget")
    DEFINE   -> aceString("jumpModeDefine")
  } ?: "Unknown"
}
