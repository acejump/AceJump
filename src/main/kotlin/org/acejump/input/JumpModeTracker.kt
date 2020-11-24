package org.acejump.input

import org.acejump.config.AceConfig

/**
 * Remembers the current [JumpMode] for a session. Allows cycling [JumpMode]s according to the order defined in configuration, or toggling
 * one specific [JumpMode] on or off.
 */
internal class JumpModeTracker {
  private var currentMode = JumpMode.DISABLED
  private var currentIndex = 0
  
  /**
   * Switches to the next [JumpMode] defined in configuration, skipping any [JumpMode]s that are not assigned. If already at the last
   * [JumpMode] in the cycle order, it resets to [JumpMode.DISABLED].
   */
  fun cycle(): JumpMode {
    val cycleModes = AceConfig.cycleModes
    
    for (testModeIndex in (currentIndex + 1)..(cycleModes.size)) {
      if (cycleModes[testModeIndex - 1] != JumpMode.DISABLED) {
        currentMode = cycleModes[testModeIndex - 1]
        currentIndex = testModeIndex
        return currentMode
      }
    }
    
    currentMode = JumpMode.DISABLED
    currentIndex = 0
    return currentMode
  }
  
  /**
   * Switches to the specified [JumpMode]. If the current mode already equals the specified one, it resets to [JumpMode.DISABLED].
   */
  fun toggle(newMode: JumpMode): JumpMode {
    if (currentMode == newMode) {
      currentMode = JumpMode.DISABLED
      currentIndex = 0
    }
    else {
      currentMode = newMode
      currentIndex = AceConfig.cycleModes.indexOfFirst { it == newMode } + 1
    }
    
    return currentMode
  }
}
