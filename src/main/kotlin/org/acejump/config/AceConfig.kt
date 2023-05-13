package org.acejump.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.application
import org.acejump.input.KeyLayoutCache

/**
 * Ensures consistency between [AceSettings] and [AceSettingsPanel].
 * Persists the state of the AceJump IDE settings across IDE restarts.
 * [https://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html]
 */
@State(name = "AceConfig", storages = [(Storage("\$APP_CONFIG\$/AceJump.xml"))])
class AceConfig: PersistentStateComponent<AceSettings> {
  private var aceSettings = AceSettings()

  companion object {
    val settings get() = application.getService(AceConfig::class.java).aceSettings

    // @formatter:off
    val layout get()              = settings.layout
    val cycleModes get()          = settings.let { arrayOf(it.cycleMode1, it.cycleMode2, it.cycleMode3, it.cycleMode4) }
    val minQueryLength get()      = settings.minQueryLength
    val jumpModeColor get()       = settings.getJumpModeJBC()
    val jumpEndModeColor get()    = settings.getJumpEndModeJBC()
    val targetModeColor get()     = settings.getTargetModeJBC()
    val definitionModeColor get() = settings.getDefinitionModeJBC()
    val textHighlightColor get()  = settings.getTextHighlightJBC()
    val tagForegroundColor get()  = settings.getTagForegroundJBC()
    val tagBackgroundColor get()  = settings.getTagBackgroundJBC()
    val searchWholeFile get()     = settings.searchWholeFile
    val mapToASCII get()        = settings.mapToASCII
    val showSearchNotification get()          = settings.showSearchNotification
    // @formatter:on
  }

  override fun getState() = aceSettings

  override fun loadState(state: AceSettings) {
    aceSettings = state
    KeyLayoutCache.reset(state)
  }
}
