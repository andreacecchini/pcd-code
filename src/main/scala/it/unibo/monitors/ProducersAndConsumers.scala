package it.unibo.monitors

import it.unibo.log.ThreadLogger.*

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import scala.collection.mutable
import it.unibo.utils.ConcurrencyUtils.*

object ProducersAndConsumers:

  trait BoundedBuffer[A]:
    def put(elem: A): Unit
    def take(): A

  trait Producer[A](using bb: BoundedBuffer[A]) extends Thread:
    protected def produce(): A
    override def run(): Unit =
      loop:
        val elem: A = produce()
        bb.put(elem)

  trait Consumer[A](using bb: BoundedBuffer[A]) extends Thread:
    protected def consume(elem: A): Unit
    override def run(): Unit =
      loop:
        val elem: A = bb.take()
        consume(elem)

  object BoundedBuffer:
    def apply[A](capacity: Int): BoundedBuffer[A] =
      new BoundedBuffer[A]:
        private val _queue = mutable.Queue[A]()
        private val _lock = ReentrantLock()
        private val _isEmpty = _lock.newCondition()
        private val _isFull = _lock.newCondition()

        given ReentrantLock = _lock
        override def put(elem: A): Unit =
          criticalSection:
            while _queue.size == capacity do _isFull.await()
            _queue.enqueue(elem)
            log(s"put $elem...")
            _isEmpty.signal()
        override def take(): A =
          criticalSection:
            while _queue.isEmpty do _isEmpty.await()
            val elem: A = _queue.dequeue()
            log(s"take $elem...")
            _isFull.signal()
            elem


  object Producer:
    private val producingTime = 1000
    private val counter = AtomicInteger(0)
    def apply(i: Int)(using bb: BoundedBuffer[Int]): Producer[Int] =
      new Producer[Int]:
        setName(s"Producer-$i")
        override def produce(): Int =
          work(producingTime)
          counter.getAndIncrement()


  object Consumer:
    private val consumingTime = 5000
    def apply(i: Int)(using bb: BoundedBuffer[Int]): Consumer[Int] =
      new Consumer[Int]:
        setName(s"Consumer-$i")
        override def consume(elem: Int): Unit =
          work(consumingTime)
          log(s"consumed ${elem.toString}...")


end ProducersAndConsumers

@main def testProducersAndConsumers(): Unit =
  import ProducersAndConsumers.*
  val nProducers = 2
  val nConsumers = 5
  val capacity = 2
  val bb: BoundedBuffer[Int] = BoundedBuffer(capacity)

  given BoundedBuffer[Int] = bb
  for i <- 1 to nProducers do Producer(i).start()
  for i <- 1 to nConsumers do Consumer(i).start()
