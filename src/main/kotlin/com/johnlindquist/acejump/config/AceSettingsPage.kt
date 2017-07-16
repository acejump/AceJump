package com.johnlindquist.acejump.config

import com.intellij.ui.ColorPanel
import java.awt.Color
import javax.swing.JPanel
import javax.swing.JTextField
import kotlin.reflect.KProperty

class AceSettingsPage {
  private lateinit var tagCharacters: JTextField
  private lateinit var jumpModeColorChooser: ColorPanel
  private lateinit var targetModeColorChooser: ColorPanel
  private lateinit var textHighlightColorChooser: ColorPanel
  private lateinit var tagForegroundColorChooser: ColorPanel
  private lateinit var tagBackgroundColorChooser: ColorPanel
  lateinit var rootPanel: JPanel

  var allowedChars: List<Char>
    get() = tagCharacters.text.toLowerCase().toList().distinct()
    set(value) = tagCharacters.setText(value.joinToString(""))

  var jumpModeColor by jumpModeColorChooser
  var targetModeColor by targetModeColorChooser
  var textHighlightColor by textHighlightColorChooser
  var tagForegroundColor by tagForegroundColorChooser
  var tagBackgroundColor by tagBackgroundColorChooser

  // Removal pending support for https://youtrack.jetbrains.com/issue/KT-8658
  private operator fun ColorPanel.getValue(a: AceSettingsPage, p: KProperty<*>) = selectedColor
  private operator fun ColorPanel.setValue(a: AceSettingsPage, p: KProperty<*>, c: Color?) { selectedColor = c }
}