package com.johnlindquist.acejump.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.Configurable
import com.johnlindquist.acejump.ui.AceUI.Settings
import javax.swing.JComponent

@State(name = "AceConfig", storages = arrayOf(Storage("AceJump.xml")))
object AceConfig : Configurable, PersistentStateComponent<Settings> {
  override fun getState() = settings
  override fun loadState(state: Settings) {
    settings = state
  }
  
  var settings = Settings()
  private var gui = AceSettingsPage()

  override fun getDisplayName() = "AceJump"

  override fun createComponent(): JComponent =
    AceSettingsPage().apply { gui = this }.rootPanel

  override fun isModified() =
    gui.allowedChars != settings.allowedChars ||
      gui.jumpModeColor != settings.jumpModeColor ||
      gui.targetModeColor != settings.targetModeColor ||
      gui.textHighlightColor != settings.textHighLightColor ||
      gui.tagForegroundColor != settings.tagForegroundColor ||
      gui.tagBackgroundColor != settings.tagBackgroundColor

  override fun apply() {
    settings.allowedChars = gui.allowedChars
    gui.jumpModeColor?.let { settings.jumpModeColor = it }
    gui.targetModeColor?.let { settings.targetModeColor = it }
    gui.textHighlightColor?.let { settings.textHighLightColor = it }
    gui.tagForegroundColor?.let { settings.tagForegroundColor = it }
    gui.tagBackgroundColor?.let { settings.tagBackgroundColor = it }
  }

  override fun reset() {
    gui.allowedChars = settings.allowedChars
    gui.jumpModeColor = settings.jumpModeColor
    gui.targetModeColor = settings.targetModeColor
    gui.textHighlightColor = settings.textHighLightColor
    gui.tagForegroundColor = settings.tagForegroundColor
    gui.tagBackgroundColor = settings.tagBackgroundColor
  }
}
