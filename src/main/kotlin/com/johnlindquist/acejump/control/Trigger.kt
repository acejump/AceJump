package com.johnlindquist.acejump.control

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.concurrency.runAsync
import java.lang.System.currentTimeMillis

/**
 * Timer for triggering events with a designated delay.
 */

object Trigger : () -> Unit {
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
        Thread.sleep(Math.abs(delay - (currentTimeMillis() - timer)))

      try {
        invokable.invoke()
      } catch (e: Exception) {
        logger.error("Exception occurred while triggering event!", e)
      }

      isRunning = false
    }
  }

  operator fun invoke(withDelay: Long = 750, function: () -> Unit) {
    this.delay = withDelay
    invokable = function
    runAsync(this)
  }
}