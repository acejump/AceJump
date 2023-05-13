package org.acejump.config

import com.intellij.ui.JBColor
import org.acejump.input.*
import org.acejump.input.KeyLayout.QWERTY

data class AceSettings(
  var layout: KeyLayout = QWERTY,
  var allowedChars: String = layout.allChars,
  var cycleMode1: JumpMode = JumpMode.JUMP,
  var cycleMode2: JumpMode = JumpMode.DECLARATION,
  var cycleMode3: JumpMode = JumpMode.TARGET,
  var cycleMode4: JumpMode = JumpMode.JUMP_END,
  var minQueryLength: Int = 1,

  var jumpModeColor: Int = 0xFFFFFF,

  var jumpEndModeColor: Int = 0x33E78A,

  var targetModeColor: Int = 0xFFB700,

  var definitionModeColor: Int = 0x6FC5FF,

  var textHighlightColor: Int = 0x394B58,

  var tagForegroundColor: Int = 0xFFFFFF,

  var tagBackgroundColor: Int = 0x008299,

  var searchWholeFile: Boolean = true,

  var mapToASCII: Boolean = false,

  var showSearchNotification: Boolean = false
) {
  fun getJumpModeJBC() = JBColor.namedColor("jumpModeRGB", jumpModeColor)
  fun getJumpEndModeJBC() = JBColor.namedColor("jumpEndModeRGB", jumpEndModeColor)
  fun getTargetModeJBC() = JBColor.namedColor("targetModeRGB", targetModeColor)
  fun getDefinitionModeJBC() = JBColor.namedColor("definitionModeRGB", definitionModeColor)
  fun getTextHighlightJBC() = JBColor.namedColor("textHighlightRGB", textHighlightColor)
  fun getTagForegroundJBC() = JBColor.namedColor("tagForegroundRGB", tagForegroundColor)
  fun getTagBackgroundJBC() = JBColor.namedColor("tagBackgroundRGB", tagBackgroundColor)
}
