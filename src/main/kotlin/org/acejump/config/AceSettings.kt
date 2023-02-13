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

  var jumpModeColor: JBColor = JBColor.namedColor("jumpModeRGB", 0xFFFFFF),

  var jumpEndModeColor: JBColor = JBColor.namedColor("jumpEndModeRGB", 0x33E78A),

  var targetModeColor: JBColor = JBColor.namedColor("targetModeRGB", 0xFFB700),

  var definitionModeColor: JBColor = JBColor.namedColor("definitionModeRGB", 0x6FC5FF),

  var textHighlightColor: JBColor = JBColor.namedColor("textHighlightRGB", 0x394B58),

  var tagForegroundColor: JBColor = JBColor.namedColor("tagForegroundRGB", 0xFFFFFF),

  var tagBackgroundColor: JBColor = JBColor.namedColor("tagBackgroundRGB", 0x008299),

  var searchWholeFile: Boolean = true,

  var mapToASCII: Boolean = false,

  var showSearchNotification: Boolean = false
)
