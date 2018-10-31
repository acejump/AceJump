package org.acejump.config

import com.intellij.ui.ColorPanel
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.GrowPolicy.MEDIUM_TEXT
import com.intellij.ui.layout.GrowPolicy.SHORT_TEXT
import com.intellij.ui.layout.panel
import org.acejump.search.aceString
import java.awt.Color
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import kotlin.reflect.KProperty

class AceSettingsPanel {
  private val tagCharactersField =
    JTextField().apply { font = Font("monospaced", font.style, font.size) }
  private val jumpModeColorWheel = ColorPanel()
  private val targetModeColorWheel = ColorPanel()
  private val textHighlightColorWheel = ColorPanel()
  private val tagForegroundColorWheel = ColorPanel()
  private val tagBackgroundColorWheel = ColorPanel()
  private val keyboardCharsLayout = JTextArea()
    .apply {
      font = Font("monospaced", font.style, font.size)
      isEditable = false
    }

  internal val rootPanel: JPanel = panel {
    fun Cell.short(component: JComponent) = component(growPolicy = SHORT_TEXT)
    fun Cell.medium(component: JComponent) = component(growPolicy = MEDIUM_TEXT)

    noteRow(aceString("tagCharsToBeUsedHeading")) { SeparatorComponent() }
    row(aceString("tagCharsToBeUsedLabel")) { medium(tagCharactersField) }
    row("Keyboard layout") { medium(keyboardCharsLayout) }
    noteRow(aceString("colorsToBeUsedHeading")) { SeparatorComponent() }
    row(aceString("jumpModeColorLabel")) { short(jumpModeColorWheel) }
    row(aceString("tagBackgroundColorLabel")) { short(tagBackgroundColorWheel) }
    row(aceString("tagForegroundColorLabel")) { short(tagForegroundColorWheel) }
    row(aceString("targetModeColorLabel")) { short(targetModeColorWheel) }
    row(aceString("textHighlightColorLabel")) { short(textHighlightColorWheel) }
  }

  internal var allowedChars: String
    get() = tagCharactersField.text.toLowerCase()
    set(value) = tagCharactersField.setText(value)

  internal var keyboardChars: String
    get() = keyboardCharsLayout.text.toLowerCase()
    set(value) = keyboardCharsLayout.setText(value)

  internal var jumpModeColor by jumpModeColorWheel
  internal var targetModeColor by targetModeColorWheel
  internal var textHighlightColor by textHighlightColorWheel
  internal var tagForegroundColor by tagForegroundColorWheel
  internal var tagBackgroundColor by tagBackgroundColorWheel

  fun reset(settings: AceSettings) {
    allowedChars = settings.allowedChars
    keyboardChars = settings.keyboardChars
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