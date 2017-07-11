package com.johnlindquist.acejump.settings

import com.intellij.openapi.components.State
import com.intellij.openapi.options.Configurable
import com.johnlindquist.acejump.ui.AceUI.defaults
import com.johnlindquist.acejump.ui.AceUI.gui
import com.johnlindquist.acejump.ui.AceUI.settings
import javax.swing.JComponent

@State(name = "AceConfig")
object AceConfig : Configurable {
//  override fun getState() = settings
//  override fun loadState(state: UserSettings) {settings = state}
//
//  override fun getId() = "preferences.AceConfigurable"

  override fun getDisplayName() = "AceJump Config"

  override fun createComponent(): JComponent {
    gui = AceSettingsPage()
    reset()
    return gui.rootPanel
  }

  override fun isModified() = settings != defaults

  override fun apply() {
    settings.allowedChars = gui.allowedChars
    settings.jumpModeColor = gui.jumpModeColor ?: defaults.jumpModeColor
    settings.targetModeColor = gui.targetModeColor ?: defaults.targetModeColor
    settings.textHighLightColor = gui.textHighlighterColor ?: defaults.textHighLightColor
    settings.tagForegroundColor = gui.tagForegroundColor ?: defaults.tagForegroundColor
    settings.tagBackgroundColor = gui.tagBackgroundColor ?: defaults.tagBackgroundColor
  }

  override fun reset() {
    gui.allowedChars = defaults.allowedChars
    gui.allowedChars
    gui.jumpModeColor = defaults.jumpModeColor
    gui.targetModeColor = defaults.targetModeColor
    gui.textHighlighterColor = defaults.textHighLightColor
    gui.tagForegroundColor = defaults.tagForegroundColor
    gui.tagBackgroundColor = defaults.tagBackgroundColor
  }
}
