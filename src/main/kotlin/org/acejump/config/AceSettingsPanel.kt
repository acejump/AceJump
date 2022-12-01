package org.acejump.config

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import org.acejump.input.JumpMode
import org.acejump.input.KeyLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.text.JTextComponent
import kotlin.reflect.KProperty

/**
 * Settings view located in File | Settings | Tools | AceJump.
 */
@Suppress("UsePropertyAccessSyntax")
internal class AceSettingsPanel {
  private val tagCharsField = JBTextField()
  private val keyboardLayoutCombo = ComboBox<KeyLayout>()
  private val keyboardLayoutArea = JBTextArea().apply { isEditable = false }
  private val cycleModeCombo1 = ComboBox<JumpMode>()
  private val cycleModeCombo2 = ComboBox<JumpMode>()
  private val cycleModeCombo3 = ComboBox<JumpMode>()
  private val cycleModeCombo4 = ComboBox<JumpMode>()
  private val minQueryLengthField = JBTextField()
  private val jumpModeColorWheel = ColorPanel()
  private val jumpEndModeColorWheel = ColorPanel()
  private val targetModeColorWheel = ColorPanel()
  private val definitionModeColorWheel = ColorPanel()
  private val textHighlightColorWheel = ColorPanel()
  private val tagForegroundColorWheel = ColorPanel()
  private val tagBackgroundColorWheel = ColorPanel()
  private val searchWholeFileCheckBox = JBCheckBox()
  private val mapToASCIICheckBox = JBCheckBox()
  private val showSearchNotificationCheckBox = JBCheckBox()

  init {
    tagCharsField.apply { font = Font("monospaced", font.style, font.size) }
    keyboardLayoutArea.apply { font = Font("monospaced", font.style, font.size) }
    keyboardLayoutCombo.setupEnumItems { keyChars = it.rows.joinToString("\n") }
    cycleModeCombo1.setupEnumItems { cycleMode1 = it }
    cycleModeCombo2.setupEnumItems { cycleMode2 = it }
    cycleModeCombo3.setupEnumItems { cycleMode3 = it }
    cycleModeCombo4.setupEnumItems { cycleMode4 = it }
  }

  internal val rootPanel: JPanel = panel {

    group("Characters and Layout") {
      row("Allowed characters in tags:") { cell(tagCharsField).columns(COLUMNS_LARGE) }
      row("Keyboard layout:") { cell(keyboardLayoutCombo).columns(COLUMNS_SHORT) }
      row("Keyboard design:") { cell(keyboardLayoutArea).columns(COLUMNS_SHORT) }
    }

    group("Modes") {
      row("Cycle order:") { cycleModeCombo1 }
      row("") {
          cycleModeCombo2
          cycleModeCombo3
          cycleModeCombo4
      }
    }

    group("Colors") {
      row("Jump mode caret background:") { cell(jumpModeColorWheel) }
      row("Jump to End mode caret background:") { cell(jumpEndModeColorWheel) }
      row("Target mode caret background:") { cell(targetModeColorWheel) }
      row("Definition mode caret background:") { cell(definitionModeColorWheel) }
      row("Searched text background:") { cell(textHighlightColorWheel) }
      row("Tag foreground:") { cell(tagForegroundColorWheel) }
      row("Tag background:") { cell(tagBackgroundColorWheel) }
    }

    group("Behavior") {
      row { cell(searchWholeFileCheckBox.apply { text = "Search whole file" }) }
      row("Minimum typed characters (1-10):") { cell(minQueryLengthField) }
    }
    group("Language Settings") {
      row { cell(mapToASCIICheckBox.apply { text = "Map unicode to ASCII" }) }
    }
    group("Visual") {
      row { cell(showSearchNotificationCheckBox.apply { text = "Show hint with search text" }) }
    }
  }

  // Property-to-property delegation: https://stackoverflow.com/q/45074596/1772342
  internal var allowedChars by tagCharsField
  internal var keyboardLayout by keyboardLayoutCombo
  internal var keyChars by keyboardLayoutArea
  internal var cycleMode1 by cycleModeCombo1
  internal var cycleMode2 by cycleModeCombo2
  internal var cycleMode3 by cycleModeCombo3
  internal var cycleMode4 by cycleModeCombo4
  internal var minQueryLength by minQueryLengthField
  internal var jumpModeColor by jumpModeColorWheel
  internal var jumpEndModeColor by jumpEndModeColorWheel
  internal var targetModeColor by targetModeColorWheel
  internal var definitionModeColor by definitionModeColorWheel
  internal var textHighlightColor by textHighlightColorWheel
  internal var tagForegroundColor by tagForegroundColorWheel
  internal var tagBackgroundColor by tagBackgroundColorWheel
  internal var searchWholeFile by searchWholeFileCheckBox
  internal var mapToASCII by mapToASCIICheckBox
  internal var showSearchNotification by showSearchNotificationCheckBox

  internal var minQueryLengthInt
    get() = minQueryLength.toIntOrNull()?.coerceIn(1, 10)
    set(value) {
      minQueryLength = value.toString()
    }

  fun reset(settings: AceSettings) {
    allowedChars = settings.allowedChars
    keyboardLayout = settings.layout
    cycleMode1 = settings.cycleMode1
    cycleMode2 = settings.cycleMode2
    cycleMode3 = settings.cycleMode3
    cycleMode4 = settings.cycleMode4
    minQueryLength = settings.minQueryLength.toString()
    jumpModeColor = settings.jumpModeColor
    jumpEndModeColor = settings.jumpEndModeColor
    targetModeColor = settings.targetModeColor
    definitionModeColor = settings.definitionModeColor
    textHighlightColor = settings.textHighlightColor
    tagForegroundColor = settings.tagForegroundColor
    tagBackgroundColor = settings.tagBackgroundColor
    searchWholeFile = settings.searchWholeFile
    mapToASCII = settings.mapToASCII
    showSearchNotification = settings.showSearchNotification
  }

  // Removal pending support for https://youtrack.jetbrains.com/issue/KT-8575

  private operator fun JTextComponent.getValue(a: AceSettingsPanel, p: KProperty<*>) = text.lowercase()
  private operator fun JTextComponent.setValue(a: AceSettingsPanel, p: KProperty<*>, s: String) = setText(s)

  private operator fun ColorPanel.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedColor
  private operator fun ColorPanel.setValue(a: AceSettingsPanel, p: KProperty<*>, c: Color?) = setSelectedColor(c)

  private operator fun JCheckBox.getValue(a: AceSettingsPanel, p: KProperty<*>) = isSelected
  private operator fun JCheckBox.setValue(a: AceSettingsPanel, p: KProperty<*>, selected: Boolean) = setSelected(selected)

  private inline operator fun <reified T> ComboBox<T>.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedItem as T
  private operator fun <T> ComboBox<T>.setValue(a: AceSettingsPanel, p: KProperty<*>, item: T) = setSelectedItem(item)

  private inline fun <reified T: Enum<T>> ComboBox<T>.setupEnumItems(crossinline onChanged: (T) -> Unit) {
    T::class.java.enumConstants.forEach(this::addItem)
    addActionListener { onChanged(selectedItem as T) }
  }
}
