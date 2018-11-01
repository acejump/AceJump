package org.acejump.config

import org.acejump.label.Pattern.Companion.KeyLayout
import java.awt.Color
import java.awt.Color.*
import kotlin.reflect.KProperty

data class AceSettings(var layout: KeyLayout = KeyLayout.QWERTY,
                       var allowedChars: String = layout.allChars(),
  // These must be primitives in order to be serializable
                       internal var jumpModeRGB: Int = BLUE.rgb,
                       internal var targetModeRGB: Int = RED.rgb,
                       internal var definitionModeRGB: Int = MAGENTA.rgb,
                       internal var textHighlightRGB: Int = GREEN.rgb,
                       internal var tagForegroundRGB: Int = BLACK.rgb,
                       internal var tagBackgroundRGB: Int = YELLOW.rgb) {

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