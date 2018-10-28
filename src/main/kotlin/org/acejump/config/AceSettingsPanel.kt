package org.acejump.config

import com.intellij.ui.ColorPanel
import com.intellij.ui.layout.GrowPolicy.MEDIUM_TEXT
import com.intellij.ui.layout.GrowPolicy.SHORT_TEXT
import com.intellij.ui.layout.panel
import org.acejump.search.aceString
import java.awt.Color
import javax.swing.JPanel
import javax.swing.JTextField
import kotlin.reflect.KProperty

class AceSettingsPanel {
  private var tagCharactersChooser = JTextField()
  private var jumpModeColorChooser = ColorPanel()
  private var targetModeColorChooser = ColorPanel()
  private var textHighlightColorChooser = ColorPanel()
  private var tagForegroundColorChooser = ColorPanel()
  private var tagBackgroundColorChooser = ColorPanel()

  val rootPanel: JPanel = panel {
    noteRow(aceString("charsToBeUsedLabel"))
    row("Chars to use:") { tagCharactersChooser(growPolicy = MEDIUM_TEXT) }
    noteRow(aceString("colorsToBeUsedLabel"))
    row(aceString("jumpModeColorLabel")) { jumpModeColorChooser(growPolicy = SHORT_TEXT) }
    row(aceString("tagBackgroundColorLabel")) { tagBackgroundColorChooser(growPolicy = SHORT_TEXT) }
    row(aceString("tagForegroundColorLabel")) { tagForegroundColorChooser(growPolicy = SHORT_TEXT) }
    row(aceString("targetModeColorLabel")) { targetModeColorChooser(growPolicy = SHORT_TEXT) }
    row(aceString("textHighlightColorLabel")) { textHighlightColorChooser(growPolicy = SHORT_TEXT) }
  }

  var allowedChars: String
    get() = tagCharactersChooser.text.toLowerCase()
    set(value) = tagCharactersChooser.setText(value)

  var jumpModeColor by jumpModeColorChooser
  var targetModeColor by targetModeColorChooser
  var textHighlightColor by textHighlightColorChooser
  var tagForegroundColor by tagForegroundColorChooser
  var tagBackgroundColor by tagBackgroundColorChooser

  fun reset(settings: AceSettings) {
    allowedChars = settings.allowedChars
    jumpModeColor = settings.jumpModeColor
    targetModeColor = settings.targetModeColor
    textHighlightColor = settings.textHighlightColor
    tagForegroundColor = settings.tagForegroundColor
    tagBackgroundColor = settings.tagBackgroundColor
  }

  // Removal pending support for https://youtrack.jetbrains.com/issue/KT-8575
  private operator fun ColorPanel.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedColor

  private operator fun ColorPanel.setValue(a: AceSettingsPanel, p: KProperty<*>, c: Color?) {
    selectedColor = c
  }
}