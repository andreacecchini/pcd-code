package it.unibo.monitors

import it.unibo.log.ThreadLogger.*

import it.unibo.utils.ConcurrencyUtils.*

object Semaphores:
  /** A [[Sempaphore]] built using Monitor. */
  class Semaphore(private var s: Int):
    def waiting(): Unit = synchronized:
      while s == 0 do wait()
      s = s - 1
    def signaling(): Unit = synchronized:
      s = s + 1
      notify()

  /** A Mutex built using [[Semaphore]]. */
  class Mutex:
    private val _semaphore = Semaphore(1)
    private var _current: Option[Thread] = Option.empty
    def acquire(): Unit =
      _semaphore.waiting()
      _current = Some(Thread.currentThread())
    def release(): Unit = _current match
      case Some(thread) if thread == Thread.currentThread() => _semaphore.signaling()
      case _ =>
  given mutexLock(using m: Mutex): Lockable[Mutex] with
    override def acquire(): Unit = m.acquire()
    override def release(): Unit = m.release()

end Semaphores

import Semaphores.{*, given}

given Mutex()
class Agent(name: String) extends Thread:
  setName(name)
  override def run(): Unit =
    loop:
      log("NCS")
      work(Agent.NCS_TIME)
      criticalSection:
        log("Entered CS...")
        work(Agent.CS_TIME)
        log("Leaving CS...")

object Agent:
  val NCS_TIME: Int = 500
  val CS_TIME: Int = 5000

@main def testSemaphoreByMonitors(): Unit =
  val agents = Seq(Agent("A"), Agent("B"), Agent("C"))
  agents foreach (_.start())
