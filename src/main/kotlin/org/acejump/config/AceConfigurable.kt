package org.acejump.config

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import org.acejump.config.AceConfig.Companion.bigrams
import org.acejump.config.AceConfig.Companion.settings

class AceConfigurable: Configurable {
  private val logger = Logger.getInstance(AceConfigurable::class.java)

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
      AceConfig.allPossibleTags = it.bigrams()
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