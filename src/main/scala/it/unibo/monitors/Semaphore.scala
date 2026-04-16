package it.unibo.monitors

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

  def mutex: Semaphore = apply(1)
  def eventSemaphore: Semaphore = apply(0)
  def resourceSemaphore(n: Int): Semaphore = apply(n)

@main def testSemaphore(): Unit =
  // Binary semaphore allowing mutual exclusion
  val mutex = Semaphore(1)
  // Event semaphore allowing synchronization
  val events = Semaphore(0)
  // Resource semaphore
  val n = 10
  val resources = Semaphore(n)
