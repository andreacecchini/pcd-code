package it.unibo.utils

import java.util.concurrent.locks.ReentrantLock

object ConcurrencyUtils:
  trait Lockable[L]:
    def acquire(): Unit
    def release(): Unit

  given reentrantLockLockable(using lock: ReentrantLock): Lockable[ReentrantLock] with
    override def acquire(): Unit = lock.lock()
    override def release(): Unit = lock.unlock()

  def loop(body: => Unit): Unit = while true do body
  def criticalSection[L: Lockable, T](body: => T): T =
    summon[Lockable[L]].acquire()
    try
      body
    finally
      summon[Lockable[L]].release()
  def work(millis: Int): Unit = Thread.sleep(millis)
