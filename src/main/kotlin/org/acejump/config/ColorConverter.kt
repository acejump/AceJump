package org.acejump.config

import com.intellij.util.xmlb.Converter
import java.awt.Color

internal class ColorConverter : Converter<Color>() {
  override fun toString(value: Color): String {
    return value.rgb.toString()
  }
  
  override fun fromString(value: String): Color? {
    return value.toIntOrNull()?.let(::Color)
  }
}
