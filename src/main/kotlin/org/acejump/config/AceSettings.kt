package org.acejump.config

import com.intellij.util.xmlb.annotations.OptionTag
import org.acejump.input.JumpMode
import org.acejump.input.KeyLayout
import org.acejump.input.KeyLayout.QWERTY
import java.awt.Color

data class AceSettings(
  var layout: KeyLayout = QWERTY,
  var allowedChars: String = layout.allChars,
  var cycleMode1: JumpMode = JumpMode.JUMP,
  var cycleMode2: JumpMode = JumpMode.DEFINE,
  var cycleMode3: JumpMode = JumpMode.TARGET,
  var cycleMode4: JumpMode = JumpMode.JUMP_END,
  
  @OptionTag("jumpModeRGB", converter = ColorConverter::class)
  var jumpModeColor: Color = Color.BLUE,
  
  @OptionTag("jumpEndModeRGB", converter = ColorConverter::class)
  var jumpEndModeColor: Color = Color.CYAN,
  
  @OptionTag("targetModeRGB", converter = ColorConverter::class)
  var targetModeColor: Color = Color.RED,
  
  @OptionTag("definitionModeRGB", converter = ColorConverter::class)
  var definitionModeColor: Color = Color.MAGENTA,
  
  @OptionTag("textHighlightRGB", converter = ColorConverter::class)
  var textHighlightColor: Color = Color.GREEN,
  
  @OptionTag("tagForegroundRGB", converter = ColorConverter::class)
  var tagForegroundColor: Color = Color.BLACK,
  
  @OptionTag("tagBackgroundRGB", converter = ColorConverter::class)
  var tagBackgroundColor: Color = Color.YELLOW,
  
  var roundedTagCorners: Boolean = true,
  var searchWholeFile: Boolean = true
)
