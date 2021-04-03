package org.acejump.input

import org.acejump.config.AceConfig

/**
 * Remembers the current [JumpMode] for a session. Allows cycling
 * [JumpMode]s according to the order defined in configuration, or
 * toggling one specific [JumpMode] on or off.
 */
internal class JumpModeTracker {
  private var currentMode = JumpMode.DISABLED
  private var currentIndex = 0

  /**
   * Switches to the next/previous [JumpMode] defined in configuration,
   * skipping any [JumpMode]s that are not assigned. If at least two
   * [JumpMode]s are assigned in the cycle order, then cycling will
   * wrap around. If only one [JumpMode] is assigned, then cycling will
   * toggle that one mode.
   */
  fun cycle(forward: Boolean): JumpMode {
    val cycleModes = AceConfig.cycleModes
    val direction = if (forward) 1 else -1
    val start = if (currentIndex == 0 && !forward) 0 else currentIndex - 1

    for (offset in 1 until cycleModes.size) {
      val index = (start + cycleModes.size + (offset * direction)) % cycleModes.size

      if (cycleModes[index] != JumpMode.DISABLED) {
        currentMode = cycleModes[index]
        currentIndex = index + 1
        return currentMode
      }
    }

    currentMode = JumpMode.DISABLED
    currentIndex = 0
    return currentMode
  }

  /**
   * Switches to the specified [JumpMode]. If the current mode already
   * equals the specified one, it resets to [JumpMode.DISABLED].
   */
  fun toggle(newMode: JumpMode): JumpMode {
    if (currentMode == newMode) {
      currentMode = JumpMode.DISABLED
      currentIndex = 0
    } else {
      currentMode = newMode
      currentIndex = AceConfig.cycleModes.indexOfFirst { it == newMode } + 1
    }

    return currentMode
  }
}
