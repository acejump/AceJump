package org.acejump.config

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColorPanel
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
  private val definitionModeColorWheel = ColorPanel()
  private val textHighlightColorWheel = ColorPanel()
  private val tagForegroundColorWheel = ColorPanel()
  private val tagBackgroundColorWheel = ColorPanel()
  private val displayQueryCheckBox = JBCheckBox().apply { isEnabled = false }
  private val searchWholeFileCheckBox = JBCheckBox()
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

    titledRow(aceString("charactersAndLayoutHeading")) {
      row(aceString("tagCharsToBeUsedLabel")) { medium(tagCharsField) }
      row(aceString("keyboardLayoutLabel")) { short(keyboardLayoutCombo) }
      row(aceString("keyboardDesignLabel")) { short(keyboardLayoutArea) }
    }

    titledRow(aceString("colorsHeading")) {
      row(aceString("jumpModeColorLabel")) { short(jumpModeColorWheel) }
      row(aceString("targetModeColorLabel")) { short(targetModeColorWheel) }
      row(aceString("definitionModeColorLabel")) { short(definitionModeColorWheel) }
      row(aceString("textHighlightColorLabel")) { short(textHighlightColorWheel) }
      row(aceString("tagForegroundColorLabel")) { short(tagForegroundColorWheel) }
      row(aceString("tagBackgroundColorLabel")) { short(tagBackgroundColorWheel) }
    }

    titledRow(aceString("appearanceHeading")) {
      row { short(displayQueryCheckBox.apply { text = aceString("displayQueryLabel") }) }
    }

    titledRow(aceString("behaviorHeading")) {
      row { short(searchWholeFileCheckBox.apply { text = aceString("searchWholeFileLabel") }) }
      row { short(supportPinyinCheckBox.apply { text = aceString("supportPinyin") }) }
    }
  }


  // Property-to-property delegation: https://stackoverflow.com/q/45074596/1772342
  internal var allowedChars by tagCharsField
  internal var keyboardLayout by keyboardLayoutCombo
  internal var keyChars by keyboardLayoutArea
  internal var jumpModeColor by jumpModeColorWheel
  internal var targetModeColor by targetModeColorWheel
  internal var definitionModeColor by definitionModeColorWheel
  internal var textHighlightColor by textHighlightColorWheel
  internal var tagForegroundColor by tagForegroundColorWheel
  internal var tagBackgroundColor by tagBackgroundColorWheel
  internal var displayQuery by displayQueryCheckBox
  internal var searchWholeFile by searchWholeFileCheckBox
  internal var supportPinyin by supportPinyinCheckBox

  fun reset(settings: AceSettings) {
    allowedChars = settings.allowedChars
    keyboardLayout = settings.layout
    jumpModeColor = settings.jumpModeColor
    targetModeColor = settings.targetModeColor
    definitionModeColor = settings.definitionModeColor
    textHighlightColor = settings.textHighlightColor
    tagForegroundColor = settings.tagForegroundColor
    tagBackgroundColor = settings.tagBackgroundColor
    displayQuery = settings.displayQuery
    searchWholeFile = settings.searchWholeFile
    supportPinyin = settings.supportPinyin
  }

  // Removal pending support for https://youtrack.jetbrains.com/issue/KT-8575
  private operator fun JTextComponent.getValue(a: AceSettingsPanel, p: KProperty<*>) = text.toLowerCase()
  private operator fun JTextComponent.setValue(a: AceSettingsPanel, p: KProperty<*>, s: String) = setText(s)

  private operator fun ColorPanel.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedColor
  private operator fun ColorPanel.setValue(a: AceSettingsPanel, p: KProperty<*>, c: Color?) = setSelectedColor(c)

  private operator fun JCheckBox.getValue(a: AceSettingsPanel, p: KProperty<*>) = isSelected
  private operator fun JCheckBox.setValue(a: AceSettingsPanel, p: KProperty<*>, selected: Boolean) = setSelected(selected)

  private operator fun <T> ComboBox<T>.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedItem as T
  private operator fun <T> ComboBox<T>.setValue(a: AceSettingsPanel, p: KProperty<*>, item: T) = setSelectedItem(item)
}