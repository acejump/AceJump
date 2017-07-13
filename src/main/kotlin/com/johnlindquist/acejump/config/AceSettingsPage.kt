package com.johnlindquist.acejump.config

import com.intellij.ui.ColorPanel
import java.awt.Color
import javax.swing.JPanel
import javax.swing.JTextField
import kotlin.reflect.KProperty

class AceSettingsPage {
  lateinit var tagCharacters: JTextField
  lateinit var jumpModeColorChooser: ColorPanel
  lateinit var targetModeColorChooser: ColorPanel
  lateinit var textHighlightColorChooser: ColorPanel
  lateinit var tagForegroundColorChooser: ColorPanel
  lateinit var tagBackgroundColorChooser: ColorPanel
  lateinit var rootPanel: JPanel

  var allowedChars: List<Char>
    get() = tagCharacters.text.toLowerCase().toList().distinct()
    set(value) = tagCharacters.setText(value.joinToString(""))

  var jumpModeColor by jumpModeColorChooser
  var targetModeColor by targetModeColorChooser
  var textHighlightColor by textHighlightColorChooser
  var tagForegroundColor by tagForegroundColorChooser
  var tagBackgroundColor by tagBackgroundColorChooser

  private operator fun ColorPanel.getValue(a: AceSettingsPage, p: KProperty<*>) = selectedColor
  private operator fun ColorPanel.setValue(a: AceSettingsPage, p: KProperty<*>, c: Color?) { selectedColor = c }
}