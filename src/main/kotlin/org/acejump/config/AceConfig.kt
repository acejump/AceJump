package org.acejump.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.diagnostic.Logger
import javax.swing.JComponent

import java.awt.Color
import java.awt.Color.*
import kotlin.reflect.KProperty

/* Persists the state of the AceJump IDE settings across IDE restarts.
 * https://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
 */

@State(name = "AceConfig", storages = [(Storage("AceJump.xml"))])
object AceConfig : Configurable, PersistentStateComponent<AceConfig.Settings> {
  data class Settings(var allowedChars: String =
                        ('a'..'z').plus('0'..'9').joinToString(""),
                      // These must be primitives in order to be serializable...
                      internal var jumpModeRGB: Int = BLUE.rgb,
                      internal var targetModeRGB: Int = RED.rgb,
                      internal var definitionModeRGB: Int = MAGENTA.rgb,
                      internal var textHighlightRGB: Int = GREEN.rgb,
                      internal var tagForegroundRGB: Int = BLACK.rgb,
                      internal var tagBackgroundRGB: Int = YELLOW.rgb) {

    // ...but we expose them to the world as Color
    val jumpModeColor: Color by { jumpModeRGB }
    val targetModeColor: Color by { targetModeRGB }
    val definitionModeColor: Color by { definitionModeRGB }
    val textHighlightColor: Color by { textHighlightRGB }
    val tagForegroundColor: Color by { tagForegroundRGB }
    val tagBackgroundColor: Color by { tagBackgroundRGB }

    // Force delegate to read the most current value by invoking as a function
    operator fun (() -> Int).getValue(s: AceConfig.Settings, p: KProperty<*>) =
      Color(this())
  }

  private val logger = Logger.getInstance(AceConfig::class.java)
  var settings: Settings = Settings()
    set(value) {
      field = value
      reset()
    }

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
    gui.jumpModeColor?.let { settings.jumpModeRGB = it.rgb }
    gui.targetModeColor?.let { settings.targetModeRGB = it.rgb }
    gui.textHighlightColor?.let { settings.textHighlightRGB = it.rgb }
    gui.tagForegroundColor?.let { settings.tagForegroundRGB = it.rgb }
    gui.tagBackgroundColor?.let { settings.tagBackgroundRGB = it.rgb }
    logger.info("User applied new settings: $settings")
  }

  override fun reset() {
    logger.info("Resetting settings to $settings")
    gui.reset(settings)
  }
}
