package org.acejump.config

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.ui.ColorPanel
import com.intellij.ui.JBColor
import java.awt.*
import javax.swing.*

internal class ResettableColorPicker(private val defaultColor: JBColor) : JPanel(FlowLayout()) {
  private val resetAction = object : AnAction({ "Reset to Default" }, AllIcons.General.Reset) {
    override fun getActionUpdateThread(): ActionUpdateThread {
      return ActionUpdateThread.EDT
    }
    
    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = colorPanel.selectedColor != defaultColor
    }
    
    override fun actionPerformed(e: AnActionEvent) {
      setSelectedColor(defaultColor)
    }
  }
  
  private val colorPanel = ColorPanel()
  private val resetButton = ActionButton(resetAction, null, ActionPlaces.UNKNOWN, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE)
  
  init {
    add(colorPanel)
    add(resetButton)
    setSelectedColor(defaultColor)
    
    colorPanel.addActionListener {
      resetButton.update()
    }
  }
  
  fun getSelectedColor(): Color? {
    return colorPanel.selectedColor
  }
  
  fun setSelectedColor(color: Color?) {
    colorPanel.selectedColor = color
    resetButton.update()
  }
}
