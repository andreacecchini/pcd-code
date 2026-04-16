package it.unibo.utils

import java.util.concurrent.locks.ReentrantLock

object ConcurrencyUtils:
  trait Lockable[L]:
    def acquire(): Unit
    def release(): Unit

  def loop(body: => Unit): Unit = while true do body

  def criticalSection[L: Lockable, T](body: => T): T =
    val lock = summon[Lockable[L]]
    lock.acquire()
    try
      body
    finally
      lock.release()

  def work(millis: Int): Unit = Thread.sleep(millis)

  // Givens
  given javaLock(using lock: ReentrantLock): Lockable[ReentrantLock] with
    override def acquire(): Unit = lock.lock()
    override def release(): Unit = lock.unlock()

