package org.acejump.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable

/* Persists the state of the AceJump IDE settings across IDE restarts.
 * https://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
 */

@State(name = "AceConfig", storages = [(Storage("AceJump.xml"))])
object AceConfig : Configurable, PersistentStateComponent<AceSettings> {
  private val logger = Logger.getInstance(AceConfig::class.java)
  var settings = AceSettings()

  override fun getState() = settings

  override fun loadState(state: AceSettings) {
    logger.info("Loaded AceConfig settings: $settings")
    settings = state
  }

  private val panel by lazy { AceSettingsPanel() }

  override fun getDisplayName() = "AceJump"

  override fun createComponent() = panel.rootPanel

  override fun isModified() =
    panel.allowedChars != settings.allowedChars ||
      panel.keyboardLayout != settings.layout ||
      panel.jumpModeColor != settings.jumpModeColor ||
      panel.targetModeColor != settings.targetModeColor ||
      panel.textHighlightColor != settings.textHighlightColor ||
      panel.tagForegroundColor != settings.tagForegroundColor ||
      panel.tagBackgroundColor != settings.tagBackgroundColor

  private fun String.distinctAlphanumerics() = toList().distinct().run {
    if (isEmpty()) settings.layout.chars() else filter { it.isLetterOrDigit() }
  }.joinToString("")

  override fun apply() {
    settings.allowedChars = panel.allowedChars.distinctAlphanumerics()
    settings.layout = panel.keyboardLayout
    panel.jumpModeColor?.let { settings.jumpModeRGB = it.rgb }
    panel.targetModeColor?.let { settings.targetModeRGB = it.rgb }
    panel.textHighlightColor?.let { settings.textHighlightRGB = it.rgb }
    panel.tagForegroundColor?.let { settings.tagForegroundRGB = it.rgb }
    panel.tagBackgroundColor?.let { settings.tagBackgroundRGB = it.rgb }
    logger.info("User applied new settings: $settings")
  }

  override fun reset() = panel.reset(settings)
}