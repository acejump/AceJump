package com.johnlindquist.acejump.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.ServiceManager.getService

@State(name = "AceConfigProvider")
class AceConfigProvider : PersistentStateComponent<AceConfigProvider.State> {
  override fun getState() = state
  private var state: State = State()

  data class State(var allowedChars: String = "abc")

  override fun loadState(state: State) {
    this.state = state
  }

  companion object {
    val instance: AceConfigProvider
      get() = getService(AceConfigProvider::class.java)
  }
}