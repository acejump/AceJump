package com.johnlindquist.acejump.settings

import com.intellij.ui.ColorPanel
import java.awt.Color
import javax.swing.JPanel
import javax.swing.JTextField

internal class AceSettingsPage {
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

  var jumpModeColor: Color?
    get() = jumpModeColorChooser.selectedColor
    set(value) { jumpModeColorChooser.selectedColor = value }

  var targetModeColor: Color?
    get() = targetModeColorChooser.selectedColor
    set(value) { targetModeColorChooser.selectedColor = value }

  var textHighlightColor: Color?
    get() = textHighlightColorChooser.selectedColor
    set(value) { textHighlightColorChooser.selectedColor = value }


  var tagForegroundColor: Color?
    get() = tagForegroundColorChooser.selectedColor
    set(value) { tagForegroundColorChooser.selectedColor = value }


  var tagBackgroundColor: Color?
    get() = tagBackgroundColorChooser.selectedColor
    set(value) { tagBackgroundColorChooser.selectedColor = value }
}
