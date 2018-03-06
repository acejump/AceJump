package com.johnlindquist.acejump.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.Configurable
import com.johnlindquist.acejump.view.Model.Settings
import javax.swing.JComponent

@State(name = "AceConfig", storages = [(Storage("AceJump.xml"))])
class AceConfig : Configurable, PersistentStateComponent<Settings> {
  companion object {
    var settings = Settings()
  }

  override fun getState() = settings
  override fun loadState(state: Settings) {
    settings = state
  }

  private var gui = AceSettingsPanel()

  override fun getDisplayName() = "AceJump"

  override fun createComponent(): JComponent =
     AceSettingsPanel().apply { gui = this }.rootPanel

  override fun isModified() =
    gui.allowedChars != settings.allowedChars ||
      gui.jumpModeColor != settings.jumpModeColor ||
      gui.targetModeColor != settings.targetModeColor ||
      gui.textHighlightColor != settings.textHighlightColor ||
      gui.tagForegroundColor != settings.tagForegroundColor ||
      gui.tagBackgroundColor != settings.tagBackgroundColor

  override fun apply() {
    settings.allowedChars = gui.allowedChars
    gui.jumpModeColor?.let { settings.jumpModeColor = it }
    gui.targetModeColor?.let { settings.targetModeColor = it }
    gui.textHighlightColor?.let { settings.textHighlightColor = it }
    gui.tagForegroundColor?.let { settings.tagForegroundColor = it }
    gui.tagBackgroundColor?.let { settings.tagBackgroundColor = it }
  }

  override fun reset() = gui.reset(settings)
}
