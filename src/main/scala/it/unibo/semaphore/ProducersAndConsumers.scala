package it.unibo.semaphore

import java.util.concurrent.atomic.AtomicInteger
import it.unibo.utils.ConcurrencyUtils.*
import it.unibo.log.ThreadLogger.*
import it.unibo.semaphore.*

import scala.collection.mutable

object ProducersAndConsumers:
  /** Represents a bounded buffer of elements of type [[A]]. */
  trait Buffer[A]:
    def put(elem: A): Unit
    def take(): A
  /** Represents a producer agent. */
  trait Producer[A](mutex: Semaphore)(availablePlaces: Semaphore)(availableItems: Semaphore)(using buff: Buffer[A]) extends Thread:
    protected def produce(): A
    override def run(): Unit =
      loop:
        val elem = produce()
        availablePlaces.waiting()
        mutex.waiting()
        buff.put(elem)
        mutex.signaling()
        log(s"put $elem...")
        availableItems.signaling()

  /** Represents a consumer agent. */
  trait Consumer[A](mutex: Semaphore)(availableItems: Semaphore)(availablePlaces: Semaphore)(using buff: Buffer[A]) extends Thread:
    protected def consume(elem: A): Unit
    override def run(): Unit =
      loop:
        availableItems.waiting()
        mutex.waiting()
        val elem = buff.take()
        mutex.signaling()
        log(s"take $elem...")
        availablePlaces.signaling()
        consume(elem)

  object Buffer:
    /** Build a [[Buffer]]. */
    def apply[A](): Buffer[A] =
      // Non atomic data structure.
      new Buffer[A]:
        private val _queue = mutable.Queue[A]()
        override def put(elem: A): Unit =
          _queue.enqueue(elem)
        override def take(): A =
          _queue.dequeue()

  object Producer:
    private val P_TIME = 1000
    private val counter = AtomicInteger(0)
    def apply(i: Int)(mutex: Semaphore)(availablePlaces: Semaphore)(availableItems: Semaphore)(using buff: Buffer[Int]): Producer[Int] =
      new Producer[Int](mutex)(availablePlaces)(availableItems):
        setName(s"Producer-$i")
        override def produce(): Int =
          work(P_TIME)
          counter.getAndIncrement()

  object Consumer:
    private val C_TIME = 5000
    def apply(i: Int)(mutex: Semaphore)(availableItems: Semaphore)(availablePlaces: Semaphore)(using buff: Buffer[Int]): Consumer[Int] =
      new Consumer[Int](mutex)(availableItems)(availablePlaces):
        setName(s"Consumer-$i")
        override def consume(elem: Int): Unit =
          work(C_TIME)
          log(s"consumed ${elem.toString}")

end ProducersAndConsumers

@main def testProducersAndConsumers(): Unit =
  import ProducersAndConsumers.*
  val nProducers = 2
  val nConsumers = 5
  val capacity = 5
  val bb: Buffer[Int] = Buffer()
  // For accessing Buffer (non-atomic)
  val mutex = Semaphore.mutex
  // Split semaphores (availableItems.V + availablePlaces.V = capacity)
  // used for synchronization
  val availableItems = Semaphore.resource(0)
  val availablePlaces = Semaphore.resource(capacity)

  given Buffer[Int] = bb
  log("Starting producers...")
  for i <- 1 to nProducers do Producer(i)(mutex)(availablePlaces)(availableItems).start()
  log("Starting consumers...")
  for i <- 1 to nConsumers do Consumer(i)(mutex)(availableItems)(availablePlaces).start()
