package org.acejump.control

import com.intellij.openapi.diagnostic.Logger
import org.acejump.search.runLater
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Timer for triggering events with a designated delay.
 */

class Trigger {
  private companion object {
    private val executor = Executors.newSingleThreadScheduledExecutor()
  }

  private val logger = Logger.getInstance(Trigger::class.java)
  private var task: Future<*>? = null

  /**
   * Can be called multiple times inside [delay], but doing so will reset the
   * timer, delaying the [event] from occurring by [withDelay] milliseconds.
   */

  @Synchronized
  operator fun invoke(withDelay: Long, event: () -> Unit = {}) {
    task?.cancel(true)
    task = executor.schedule({
      runLater {
        try {
          event()
        } catch (e: Exception) {
          logger.error("Exception occurred while triggering event!", e)
        } finally {
          task = null
        }
      }
    }, withDelay, TimeUnit.MILLISECONDS)
  }
}