package org.acejump.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import org.acejump.label.Pattern
import javax.swing.JComponent

/* Persists the state of the AceJump IDE settings across IDE restarts.
 * https://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
 */

@State(name = "AceConfig", storages = [(Storage("AceJump.xml"))])
object AceConfig : Configurable, PersistentStateComponent<AceSettings> {
  private val logger = Logger.getInstance(AceConfig::class.java)
  var settings: AceSettings = AceSettings()

  override fun getState() = settings

  override fun loadState(state: AceSettings) {
    logger.info("Loaded AceConfig settings: $settings")
    settings = state
  }

  private var gui = AceSettingsPanel()

  override fun getDisplayName() = "AceJump"

  override fun createComponent(): JComponent = AceSettingsPanel().apply { gui = this }.rootPanel

  override fun isModified() =
    gui.allowedChars != settings.allowedChars ||
      gui.jumpModeColor != settings.jumpModeColor ||
      gui.targetModeColor != settings.targetModeColor ||
      gui.textHighlightColor != settings.textHighlightColor ||
      gui.tagForegroundColor != settings.tagForegroundColor ||
      gui.tagBackgroundColor != settings.tagBackgroundColor

  override fun apply() {
    settings.allowedChars = gui.allowedChars.toList().distinct().run {
      if (isEmpty()) Pattern.defaultChars else filter { it.isLetterOrDigit() }
    }.joinToString("")

    gui.jumpModeColor?.let { settings.jumpModeRGB = it.rgb }
    gui.targetModeColor?.let { settings.targetModeRGB = it.rgb }
    gui.textHighlightColor?.let { settings.textHighlightRGB = it.rgb }
    gui.tagForegroundColor?.let { settings.tagForegroundRGB = it.rgb }
    gui.tagBackgroundColor?.let { settings.tagBackgroundRGB = it.rgb }
    logger.info("User applied new settings: $settings")
  }

  override fun reset() = gui.reset(settings)
}