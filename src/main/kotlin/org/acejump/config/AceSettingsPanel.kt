package org.acejump.config

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColorPanel
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.Cell
import com.intellij.ui.layout.GrowPolicy.MEDIUM_TEXT
import com.intellij.ui.layout.GrowPolicy.SHORT_TEXT
import com.intellij.ui.layout.panel
import org.acejump.label.Pattern.Companion.KeyLayout
import org.acejump.search.aceString
import java.awt.Color
import java.awt.Font
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.text.JTextComponent
import kotlin.reflect.KProperty

/**
 * Settings view located in File | Settings | Tools | AceJump.
 */

internal class AceSettingsPanel {
  private val tagCharsField = JBTextField()
  private val keyboardLayoutCombo = ComboBox<KeyLayout>()
  private val keyboardLayoutArea = JBTextArea()
  private val jumpModeColorWheel = ColorPanel()
  private val targetModeColorWheel = ColorPanel()
  private val textHighlightColorWheel = ColorPanel()
  private val tagForegroundColorWheel = ColorPanel()
  private val tagBackgroundColorWheel = ColorPanel()
  private val displayQueryCheckBox = JBCheckBox()
  private val supportPinyinCheckBox = JBCheckBox()

  init {
    tagCharsField.apply { font = Font("monospaced", font.style, font.size) }
    keyboardLayoutArea.apply {
      font = Font("monospaced", font.style, font.size)
      isEditable = false
    }

    keyboardLayoutCombo.run {
      KeyLayout.values().forEach { addItem(it) }
      addActionListener { keyChars = (selectedItem as KeyLayout).joinBy("\n") }
    }
  }

  // https://github.com/JetBrains/intellij-community/blob/master/platform/platform-impl/src/com/intellij/ui/layout/readme.md
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
    row(aceString("appearanceHeading")) { SeparatorComponent() }
    row(aceString("displayQueryLabel")) { short(displayQueryCheckBox) }
    row(aceString("languagesHeading")) { SeparatorComponent() }
    row(aceString("supportPinyin")) { short(supportPinyinCheckBox) }
  }

  internal var keyboardLayout: KeyLayout
    get() = keyboardLayoutCombo.selectedItem as KeyLayout
    set(value) { keyboardLayoutCombo.selectedItem = value }

  // Property-to-property delegation: https://stackoverflow.com/q/45074596/1772342
  internal var keyChars by keyboardLayoutArea
  internal var allowedChars by tagCharsField
  internal var jumpModeColor by jumpModeColorWheel
  internal var targetModeColor by targetModeColorWheel
  internal var textHighlightColor by textHighlightColorWheel
  internal var tagForegroundColor by tagForegroundColorWheel
  internal var tagBackgroundColor by tagBackgroundColorWheel
  internal var displayQuery by displayQueryCheckBox
  internal var supportPinyin by supportPinyinCheckBox

  fun reset(settings: AceSettings) {
    allowedChars = settings.allowedChars
    keyboardLayout = settings.layout
    jumpModeColor = settings.jumpModeColor
    targetModeColor = settings.targetModeColor
    textHighlightColor = settings.textHighlightColor
    tagForegroundColor = settings.tagForegroundColor
    tagBackgroundColor = settings.tagBackgroundColor
    displayQuery = settings.displayQuery
    supportPinyin = settings.supportPinyin
  }

  // Removal pending support for https://youtrack.jetbrains.com/issue/KT-8575
  private operator fun JTextComponent.getValue(a: AceSettingsPanel, p: KProperty<*>) = text.toLowerCase()
  private operator fun JTextComponent.setValue(a: AceSettingsPanel, p: KProperty<*>, s: String) = setText(s)

  private operator fun ColorPanel.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedColor
  private operator fun ColorPanel.setValue(a: AceSettingsPanel, p: KProperty<*>, c: Color?) = setSelectedColor(c)

  private operator fun JCheckBox.getValue(a: AceSettingsPanel, p: KProperty<*>) = isEnabled
  private operator fun JCheckBox.setValue(a: AceSettingsPanel, p: KProperty<*>, enabled: Boolean) = setEnabled(enabled)
}