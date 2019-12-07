package org.acejump.config

import org.acejump.label.Pattern.Companion.KeyLayout
import org.acejump.label.Pattern.Companion.KeyLayout.QWERTY
import java.awt.Color
import java.awt.Color.*
import kotlin.reflect.KProperty

// TODO: https://github.com/acejump/AceJump/issues/215
data class AceSettings(var layout: KeyLayout = QWERTY,
                       var allowedChars: String = layout.text,
                       // These must be primitives in order to be serializable
                       internal var jumpModeRGB: Int = BLUE.rgb,
                       internal var targetModeRGB: Int = RED.rgb,
                       internal var definitionModeRGB: Int = MAGENTA.rgb,
                       internal var textHighlightRGB: Int = GREEN.rgb,
                       internal var tagForegroundRGB: Int = BLACK.rgb,
                       internal var tagBackgroundRGB: Int = YELLOW.rgb,
                       internal var displayQuery: Boolean = false,
                       internal var supportPinyin: Boolean = true) {

  // ...but we expose them to the world as Color
  val jumpModeColor: Color by { jumpModeRGB }
  val targetModeColor: Color by { targetModeRGB }
  val definitionModeColor: Color by { definitionModeRGB }
  val textHighlightColor: Color by { textHighlightRGB }
  val tagForegroundColor: Color by { tagForegroundRGB }
  val tagBackgroundColor: Color by { tagBackgroundRGB }

  // Force delegate to read the most current value by invoking as a function
  operator fun (() -> Int).getValue(s: AceSettings, p: KProperty<*>) = Color(this())
}