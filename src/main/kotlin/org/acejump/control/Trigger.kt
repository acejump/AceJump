package org.acejump.control

import com.intellij.openapi.diagnostic.Logger
import org.acejump.search.runLater
import java.lang.System.currentTimeMillis
import kotlin.math.abs

/**
 * Timer for triggering events with a designated delay.
 */

object Trigger: () -> Unit {
  private val logger = Logger.getInstance(Trigger::class.java)
  private var delay = 0L
  private var timer = currentTimeMillis()
  private var isRunning = false
  private var invokable: () -> Unit = {}

  override fun invoke() {
    timer = currentTimeMillis()
    if (isRunning) return
    synchronized(this) {
      isRunning = true

      while (currentTimeMillis() - timer <= delay)
        Thread.sleep(abs(delay - (currentTimeMillis() - timer)))

      try {
        invokable()
      } catch (e: Exception) {
        logger.error("Exception occurred while triggering event!", e)
      }

      isRunning = false
    }
  }

  /**
   * Can be called multiple times inside [delay], but doing so will reset the
   * timer, delaying the [event] from occurring by [withDelay] milliseconds.
   */

  operator fun invoke(withDelay: Long = 750, event: () -> Unit = {}) {
    delay = withDelay
    invokable = event
    runLater(this)
  }
}