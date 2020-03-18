package org.acejump.config

import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import org.acejump.label.Pattern
import org.acejump.label.Pattern.Companion.KeyLayout
import java.awt.Color

/**
 * Ensures consistiency between [AceSettings] and [AceSettingsPanel].
 * Persists the state of the AceJump IDE settings across IDE restarts.
 * https://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
 */

@State(name = "AceConfig", storages = [(Storage("AceJump.xml"))])
object AceConfig: Configurable, PersistentStateComponent<AceSettings> {
  private val logger = Logger.getInstance(AceConfig::class.java)
  private var settings = AceSettings()
    set(value) {
      allPossibleTags = value.allowedChars.bigrams()
      field = value
    }

  val allowedChars: String get() = settings.allowedChars
  val layout: KeyLayout get() = settings.layout
  val jumpModeColor: Color get() = settings.jumpModeColor
  val textHighlightColor: Color get() = settings.textHighlightColor
  val targetModeColor: Color get() = settings.targetModeColor
  val definitionModeColor: Color get() = settings.definitionModeColor
  val tagForegroundColor: Color get() = settings.tagForegroundColor
  val tagBackgroundColor: Color get() = settings.tagBackgroundColor
  val supportPinyin: Boolean get() = settings.supportPinyin

  private var allPossibleTags: Set<String> = settings.allowedChars.bigrams()

  private fun String.bigrams() = run { flatMap { e -> map { c -> "$e$c" } } }
      .sortedWith(Pattern.defaultTagOrder).toSet()

  fun getCompatibleTags(query: String, matching: (String) -> Boolean) =
    Pattern.filter(allPossibleTags, query).filter(matching).toSet()

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
      panel.tagBackgroundColor != settings.tagBackgroundColor ||
      panel.supportPinyin != settings.supportPinyin

  private fun String.distinctAlphanumerics() =
    if (isEmpty()) settings.layout.text
    else toList().distinct().filter(Char::isLetterOrDigit).joinToString("")

  override fun apply() {
    panel.allowedChars.distinctAlphanumerics().let {
      settings.allowedChars = it
      allPossibleTags = it.bigrams()
    }

    settings.layout = panel.keyboardLayout
    panel.jumpModeColor?.let { settings.jumpModeRGB = it.rgb }
    panel.targetModeColor?.let { settings.targetModeRGB = it.rgb }
    panel.textHighlightColor?.let { settings.textHighlightRGB = it.rgb }
    panel.tagForegroundColor?.let { settings.tagForegroundRGB = it.rgb }
    panel.tagBackgroundColor?.let { settings.tagBackgroundRGB = it.rgb }
    panel.supportPinyin.let { settings.supportPinyin = it }
    logger.info("User applied new settings: $settings")
  }

  override fun reset() = panel.reset(settings)
}