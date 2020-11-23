package org.acejump.config

import com.intellij.util.xmlb.annotations.OptionTag
import org.acejump.label.Pattern.Companion.KeyLayout
import org.acejump.label.Pattern.Companion.KeyLayout.QWERTY
import java.awt.Color

/**
 * Settings model located for [AceSettingsPanel].
 */

// TODO: https://github.com/acejump/AceJump/issues/215
data class AceSettings(
  var layout: KeyLayout = QWERTY,
  var allowedChars: String = layout.text,

  @OptionTag("jumpModeRGB", converter = ColorConverter::class)
  var jumpModeColor: Color = Color.BLUE,

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

  var displayQuery: Boolean = false,
  var roundedTagCorners: Boolean = true,
  var supportPinyin: Boolean = false
)