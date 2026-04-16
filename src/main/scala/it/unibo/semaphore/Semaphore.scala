package it.unibo.semaphore

trait Semaphore:
  def waiting(): Unit
  def signaling(): Unit

object Semaphore:
  def apply(s: Int): Semaphore =
    // Actually implemented as a monitor
    new Semaphore:
      private var _v = s
      def waiting(): Unit = synchronized:
        while _v == 0 do wait()
        _v = _v - 1
      def signaling(): Unit = synchronized:
        _v = _v + 1
        notify()

  /** Binary [[Semaphore]] allowing mutual exclusion. */
  def mutex: Semaphore = apply(1)
  /** Event [[Semaphore]] allowing synchronization. */
  def eventSemaphore: Semaphore = apply(0)
  /** Resource [[Semaphore]]. */
  def resourceSemaphore(n: Int): Semaphore = apply(n)

