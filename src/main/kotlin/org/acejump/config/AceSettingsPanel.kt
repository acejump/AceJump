package org.acejump.config

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColorPanel
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.GrowPolicy.MEDIUM_TEXT
import com.intellij.ui.layout.GrowPolicy.SHORT_TEXT
import com.intellij.ui.layout.panel
import org.acejump.label.Pattern.Companion.KeyLayout
import org.acejump.search.aceString
import java.awt.Color
import java.awt.Font
import javax.swing.*
import javax.swing.text.JTextComponent
import kotlin.reflect.KProperty

class AceSettingsPanel {
  private val tagCharsField = JTextField()
  private val keyboardLayoutCombo = ComboBox<KeyLayout>()
  private val keyboardLayoutArea = JTextArea()
  private val jumpModeColorWheel = ColorPanel()
  private val targetModeColorWheel = ColorPanel()
  private val textHighlightColorWheel = ColorPanel()
  private val tagForegroundColorWheel = ColorPanel()
  private val tagBackgroundColorWheel = ColorPanel()

  init {
    tagCharsField.apply { font = Font("monospaced", font.style, font.size) }
    keyboardLayoutArea.apply {
      font = Font("monospaced", font.style, font.size)
      isEditable = false
    }

    keyboardLayoutCombo.run {
      KeyLayout.values().forEach { addItem(it) }
      addActionListener { keyChars = (selectedItem as KeyLayout).keyboard() }
    }
  }

  internal val rootPanel: JPanel = panel {
    fun Cell.short(component: JComponent) = component(growPolicy = SHORT_TEXT)
    fun Cell.medium(component: JComponent) = component(growPolicy = MEDIUM_TEXT)

    noteRow(aceString("tagCharsToBeUsedHeading")) { SeparatorComponent() }
    row(aceString("tagCharsToBeUsedLabel")) { medium(tagCharsField) }
    row("Keyboard layout") { short(keyboardLayoutCombo) }
    row("Keyboard design") { short(keyboardLayoutArea) }
    noteRow(aceString("colorsToBeUsedHeading")) { SeparatorComponent() }
    row(aceString("jumpModeColorLabel")) { short(jumpModeColorWheel) }
    row(aceString("tagBackgroundColorLabel")) { short(tagBackgroundColorWheel) }
    row(aceString("tagForegroundColorLabel")) { short(tagForegroundColorWheel) }
    row(aceString("targetModeColorLabel")) { short(targetModeColorWheel) }
    row(aceString("textHighlightColorLabel")) { short(textHighlightColorWheel) }
  }

  internal var keyboardLayout: KeyLayout
    get() = keyboardLayoutCombo.selectedItem as KeyLayout
    set(value) { keyboardLayoutCombo.selectedItem = value }

  internal var keyChars by keyboardLayoutArea
  internal var allowedChars by tagCharsField
  internal var jumpModeColor by jumpModeColorWheel
  internal var targetModeColor by targetModeColorWheel
  internal var textHighlightColor by textHighlightColorWheel
  internal var tagForegroundColor by tagForegroundColorWheel
  internal var tagBackgroundColor by tagBackgroundColorWheel

  fun reset(settings: AceSettings) {
    allowedChars = settings.allowedChars
    keyboardLayout = settings.layout
    jumpModeColor = settings.jumpModeColor
    targetModeColor = settings.targetModeColor
    textHighlightColor = settings.textHighlightColor
    tagForegroundColor = settings.tagForegroundColor
    tagBackgroundColor = settings.tagBackgroundColor
  }

  // Removal pending support for https://youtrack.jetbrains.com/issue/KT-8575
  private operator fun JTextComponent.getValue(a: AceSettingsPanel, p: KProperty<*>) = text.toLowerCase()
  private operator fun JTextComponent.setValue(a: AceSettingsPanel, p: KProperty<*>, s: String) = setText(s)

  private operator fun ColorPanel.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedColor
  private operator fun ColorPanel.setValue(a: AceSettingsPanel, p: KProperty<*>, c: Color?) = setSelectedColor(c)
}