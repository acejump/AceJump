package org.acejump.config

import com.intellij.openapi.options.Configurable
import org.acejump.config.AceConfig.Companion.settings
import org.acejump.input.KeyLayoutCache

class AceConfigurable: Configurable {
  private val panel by lazy(::AceSettingsPanel)

  override fun getDisplayName() = "AceJump"

  override fun createComponent() = panel.rootPanel

  override fun isModified() =
    panel.allowedChars != settings.allowedChars ||
      panel.keyboardLayout != settings.layout ||
      panel.cycleMode1 != settings.cycleMode1 ||
      panel.cycleMode2 != settings.cycleMode2 ||
      panel.cycleMode3 != settings.cycleMode3 ||
      panel.cycleMode4 != settings.cycleMode4 ||
      panel.minQueryLengthInt != settings.minQueryLength ||
      panel.jumpModeColor?.rgb != settings.jumpModeColor ||
      panel.jumpEndModeColor?.rgb != settings.jumpEndModeColor ||
      panel.targetModeColor?.rgb != settings.targetModeColor ||
      panel.definitionModeColor?.rgb != settings.definitionModeColor ||
      panel.textHighlightColor?.rgb != settings.textHighlightColor ||
      panel.tagForegroundColor?.rgb != settings.tagForegroundColor ||
      panel.tagBackgroundColor?.rgb != settings.tagBackgroundColor ||
      panel.searchWholeFile != settings.searchWholeFile ||
      panel.mapToASCII != settings.mapToASCII ||
      panel.showSearchNotification != settings.showSearchNotification

  override fun apply() {
    settings.allowedChars = panel.allowedChars
    settings.layout = panel.keyboardLayout
    settings.cycleMode1 = panel.cycleMode1
    settings.cycleMode2 = panel.cycleMode2
    settings.cycleMode3 = panel.cycleMode3
    settings.cycleMode4 = panel.cycleMode4
    settings.minQueryLength = panel.minQueryLengthInt ?: settings.minQueryLength
    panel.jumpModeColor?.let { settings.jumpModeColor = it.rgb }
    panel.jumpEndModeColor?.let { settings.jumpEndModeColor = it.rgb }
    panel.targetModeColor?.let { settings.targetModeColor = it.rgb }
    panel.definitionModeColor?.let { settings.definitionModeColor = it.rgb }
    panel.textHighlightColor?.let { settings.textHighlightColor = it.rgb }
    panel.tagForegroundColor?.let { settings.tagForegroundColor = it.rgb }
    panel.tagBackgroundColor?.let { settings.tagBackgroundColor = it.rgb }
    settings.searchWholeFile = panel.searchWholeFile
    settings.mapToASCII = panel.mapToASCII
    settings.showSearchNotification = panel.showSearchNotification
    KeyLayoutCache.reset(settings)
  }

  override fun reset() = panel.reset(settings)
}
