package org.acejump.config

import com.intellij.openapi.options.Configurable
import com.intellij.ui.JBColor
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
      panel.jumpModeColor != settings.jumpModeColor ||
      panel.jumpEndModeColor != settings.jumpEndModeColor ||
      panel.targetModeColor != settings.targetModeColor ||
      panel.definitionModeColor != settings.definitionModeColor ||
      panel.textHighlightColor != settings.textHighlightColor ||
      panel.tagForegroundColor != settings.tagForegroundColor ||
      panel.tagBackgroundColor != settings.tagBackgroundColor ||
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
    panel.jumpModeColor?.let { settings.jumpModeColor = JBColor.namedColor("jumpModeRGB", it) }
    panel.jumpEndModeColor?.let { settings.jumpEndModeColor = JBColor.namedColor("jumpEndModeRGB", it) }
    panel.targetModeColor?.let { settings.targetModeColor = JBColor.namedColor("targetModeRGB", it) }
    panel.definitionModeColor?.let { settings.definitionModeColor = JBColor.namedColor("definitionModeRGB", it) }
    panel.textHighlightColor?.let { settings.textHighlightColor = JBColor.namedColor("textHighlightRGB", it) }
    panel.tagForegroundColor?.let { settings.tagForegroundColor = JBColor.namedColor("tagForegroundRGB", it) }
    panel.tagBackgroundColor?.let { settings.tagBackgroundColor = JBColor.namedColor("tagBackgroundRGB", it) }
    settings.searchWholeFile = panel.searchWholeFile
    settings.mapToASCII = panel.mapToASCII
    settings.showSearchNotification = panel.showSearchNotification
    KeyLayoutCache.reset(settings)
  }

  override fun reset() = panel.reset(settings)
}
