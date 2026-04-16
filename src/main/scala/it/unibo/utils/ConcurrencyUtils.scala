package it.unibo.utils

import java.util.concurrent.locks.ReentrantLock

object ConcurrencyUtils:
  def loop(body: => Unit): Unit = while true do body
  def criticalSection[T](using lock: ReentrantLock)(body: => T): T =
    lock.lock()
    try
      body
    finally
      lock.unlock()
