package org.acejump.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.Configurable
import org.acejump.view.Model.Settings
import com.intellij.openapi.diagnostic.Logger
import javax.swing.JComponent

@State(name = "AceConfig", storages = [(Storage("AceJump.xml"))])
object AceConfig : Configurable, PersistentStateComponent<Settings> {
  private val logger = Logger.getInstance(AceConfig::class.java)
  var settings: Settings = Settings()

  override fun getState() = settings

  override fun loadState(state: Settings) {
    logger.info("Loaded AceConfig settings: $settings")
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
    logger.info("User applied new settings: $settings")
  }

  override fun reset() {
    logger.info("Resetting settings")
    gui.reset(settings)
  }
}
