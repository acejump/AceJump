package com.johnlindquist.acejump.config

import com.intellij.ui.ColorPanel
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.uiDesigner.core.Spacer
import com.johnlindquist.acejump.view.Model.Settings
import java.awt.Color
import java.awt.Dimension
import java.awt.Insets
import java.util.*
import javax.swing.*
import kotlin.reflect.KProperty

class AceSettingsPanel {
  private var tagCharacters: JTextField
  private var jumpModeColorChooser: ColorPanel
  private var targetModeColorChooser: ColorPanel
  private var textHighlightColorChooser: ColorPanel
  private var tagForegroundColorChooser: ColorPanel
  private var tagBackgroundColorChooser: ColorPanel
  var rootPanel: JPanel = JPanel()

  init {
    rootPanel.layout = GridLayoutManager(12, 3, Insets(0, 0, 0, 0), -1, -1)
    rootPanel.border = BorderFactory.createTitledBorder("")
    tagCharacters = JTextField()
    tagCharacters.text = ResourceBundle.getBundle("AceResources").getString("tagCharacters")
    rootPanel.add(tagCharacters, GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, Dimension(150, -1), null, 0, false))
    val spacer1 = Spacer()
    rootPanel.add(spacer1, GridConstraints(11, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false))
    val label1 = JLabel()
    loadLabelText(label1, ResourceBundle.getBundle("AceResources").getString("jumpModeColorLabel"))
    rootPanel.add(label1, GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
    jumpModeColorChooser = ColorPanel()
    rootPanel.add(jumpModeColorChooser, GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false))
    val label2 = JLabel()
    loadLabelText(label2, ResourceBundle.getBundle("AceResources").getString("targetModeColorLabel"))
    rootPanel.add(label2, GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
    targetModeColorChooser = ColorPanel()
    rootPanel.add(targetModeColorChooser, GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false))
    val label3 = JLabel()
    loadLabelText(label3, ResourceBundle.getBundle("AceResources").getString("textHighlightColorLabel"))
    rootPanel.add(label3, GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
    textHighlightColorChooser = ColorPanel()
    rootPanel.add(textHighlightColorChooser, GridConstraints(8, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false))
    val label4 = JLabel()
    loadLabelText(label4, ResourceBundle.getBundle("AceResources").getString("tagForegroundColorLabel"))
    rootPanel.add(label4, GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
    tagForegroundColorChooser = ColorPanel()
    rootPanel.add(tagForegroundColorChooser, GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false))
    val label5 = JLabel()
    loadLabelText(label5, ResourceBundle.getBundle("AceResources").getString("tagBackgroundColorLabel"))
    rootPanel.add(label5, GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
    tagBackgroundColorChooser = ColorPanel()
    rootPanel.add(tagBackgroundColorChooser, GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK or GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false))
    val label6 = JLabel()
    loadLabelText(label6, ResourceBundle.getBundle("AceResources").getString("charsToBeUsedLabel"))
    rootPanel.add(label6, GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
    val spacer2 = Spacer()
    rootPanel.add(spacer2, GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false))
    val separator1 = JSeparator()
    separator1.toolTipText = ""
    rootPanel.add(separator1, GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
    val label7 = JLabel()
    loadLabelText(label7, ResourceBundle.getBundle("AceResources").getString("colorsLabel"))
    rootPanel.add(label7, GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
    val separator2 = JSeparator()
    separator2.toolTipText = ""
    rootPanel.add(separator2, GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
  }

    /**
     * @noinspection ALL
     */
    private fun loadLabelText(component: JLabel, text: String) {
      val result = StringBuffer()
      var haveMnemonic = false
      var mnemonic = '\u0000'
      var mnemonicIndex = -1
      var i = 0
      while (i < text.length) {
        if (text[i] == '&') {
          i++
          if (i == text.length) break
          if (!haveMnemonic && text[i] != '&') {
            haveMnemonic = true
            mnemonic = text[i]
            mnemonicIndex = result.length
          }
        }
        result.append(text[i])
        i++
      }
      component.text = result.toString()
      if (haveMnemonic) {
        component.setDisplayedMnemonic(mnemonic)
        component.displayedMnemonicIndex = mnemonicIndex
      }
    }

  var allowedChars: List<Char>
    get() = tagCharacters.text.toLowerCase().toList().distinct()
    set(value) = tagCharacters.setText(value.joinToString(""))

  var jumpModeColor by jumpModeColorChooser
  var targetModeColor by targetModeColorChooser
  var textHighlightColor by textHighlightColorChooser
  var tagForegroundColor by tagForegroundColorChooser
  var tagBackgroundColor by tagBackgroundColorChooser

  fun reset(settings: Settings) {
    allowedChars = settings.allowedChars
    jumpModeColor = settings.jumpModeColor
    targetModeColor = settings.targetModeColor
    textHighlightColor = settings.textHighlightColor
    tagForegroundColor = settings.tagForegroundColor
    tagBackgroundColor = settings.tagBackgroundColor
  }

  // Removal pending support for https://youtrack.jetbrains.com/issue/KT-8658
  private operator fun ColorPanel.getValue(a: AceSettingsPanel, p: KProperty<*>) = selectedColor

  private operator fun ColorPanel.setValue(a: AceSettingsPanel, p: KProperty<*>, c: Color?) {
    selectedColor = c
  }
}