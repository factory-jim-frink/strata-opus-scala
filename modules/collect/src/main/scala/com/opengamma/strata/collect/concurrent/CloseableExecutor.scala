/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.concurrent

import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

import com.opengamma.strata.collect.ArgChecker
import com.opengamma.strata.collect.Unchecked
import com.opengamma.strata.collect.function.CheckedRunnable

/**
 * AutoCloseable wrapper around an executor.
 *
 * This shuts down the wrapped executor when it is closed, and can wait for tasks to exit.
 *
 * @constructor Restricted constructor.
 * @param rawExecutorService  the underlying executor service
 * @param rawDuration  the duration to wait
 */
class CloseableExecutor private (rawExecutorService: ExecutorService, rawDuration: Duration) extends AutoCloseable {

  /**
   * The wrapped executor service.
   */
  private val executorService: ExecutorService = ArgChecker.notNull(rawExecutorService, "executorService")
  /**
   * The duration to wait for tasks to terminate.
   */
  private val duration: Duration = ArgChecker.notNull(rawDuration, "duration")
  ArgChecker.isFalse(duration.isNegative(), "Duration to wait must be positive or zero: {}", duration)

  override def close(): Unit = {
    val action: CheckedRunnable = () => {
      executorService.shutdown()
      if (!duration.isZero()) {
        executorService.awaitTermination(duration.toMillis(), TimeUnit.MILLISECONDS)
      }
    }
    Unchecked.wrap(action)
  }
}

object CloseableExecutor {

  /**
   * Returns a closeable executor that wraps a passed in executor.
   *
   * The passed in executor is shut down when the returned CloseableExecutor is closed.
   *
   * @param executor  the executor to wrap
   * @return a CloseableExecutor
   */
  def of(executor: ExecutorService): CloseableExecutor = {
    new CloseableExecutor(executor, Duration.ZERO)
  }

  /**
   * Returns a closeable executor that wraps a passed in executor.
   *
   * The passed in executor is shut down when the returned CloseableExecutor is closed, and waits for the given
   * duration for tasks to finish.
   *
   * @param executor  the executor to wrap
   * @param duration  the duration to wait for tasks to exit
   * @return a CloseableExecutor
   */
  def of(executor: ExecutorService, duration: Duration): CloseableExecutor = {
    new CloseableExecutor(executor, duration)
  }

}
