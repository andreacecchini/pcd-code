package it.unibo.monitors

import it.unibo.log.ThreadLogger.*

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable
import it.unibo.utils.ConcurrencyUtils.*

object ProducersAndConsumers:

  class BoundedBuffer[A](capacity: Int):
    private val queue = mutable.Queue[A]()
    private val lock = ReentrantLock()
    private val isEmpty = lock.newCondition()
    private val isFull = lock.newCondition()

    given ReentrantLock = lock
    def put(elem: A): Unit =
      criticalSection:
        while queue.size == capacity do isFull.await()
        queue.enqueue(elem)
        log(s"enqueue $elem...")
        isEmpty.signal()

    def take(): A =
      criticalSection:
        while queue.isEmpty do isEmpty.await()
        val elem = queue.dequeue()
        log(s"dequeue $elem...")
        isFull.signal()
        elem

  end BoundedBuffer

  trait Producer[A](bb: BoundedBuffer[A]) extends Thread:
    protected def produce(): A
    override def run(): Unit =
      loop:
        val elem: A = produce()
        bb.put(elem)

  object Producer:
    private val producingTime = 1000
    private val counter = AtomicInteger(0)
    def apply(i: Int)(using bb: BoundedBuffer[Int]): Producer[Int] =
      new Producer[Int](bb):
        setName(s"Producer-$i")
        override def produce(): Int =
          Thread.sleep(producingTime)
          counter.getAndIncrement()

  trait Consumer[A](bb: BoundedBuffer[A]) extends Thread:
    protected def consume(elem: A): Unit
    override def run(): Unit =
      loop:
        val elem: A = bb.take()
        consume(elem)

  object Consumer:
    private val consumingTime = 5000
    def apply(i: Int)(using bb: BoundedBuffer[Int]): Consumer[Int] =
      new Consumer[Int](bb):
        setName(s"Consumer-$i")
        override def consume(elem: Int): Unit =
          Thread.sleep(consumingTime);
          log(elem.toString)


end ProducersAndConsumers

@main def testProducersAndConsumers(): Unit =
  import ProducersAndConsumers.*
  val nProducers = 2
  val nConsumers = 5
  val capacity = 2

  given BoundedBuffer[Int](capacity)
  for i <- 1 to nProducers do Producer(i).start()
  for i <- 1 to nConsumers do Consumer(i).start()
