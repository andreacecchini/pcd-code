package it.unibo.monitors

import it.unibo.log.ThreadLogger.*

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import it.unibo.utils.ConcurrencyUtils.*

import scala.reflect.ClassTag

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
    def apply[A: ClassTag](capacity: Int): BoundedBuffer[A] =
      require(capacity > 0, "capacity must be > 0")

      new BoundedBuffer[A]:
        private val _buff: Array[A] = new Array(capacity + 1)
        private var head = 0
        private var tail = 0
        private val _lock = ReentrantLock()
        private val _notEmpty = _lock.newCondition()
        private val _notFull = _lock.newCondition()
        private inline def next(i: Int): Int = (i + 1) % _buff.length
        private def isFull: Boolean = next(tail) == head
        private def isEmpty: Boolean = tail == head

        given ReentrantLock = _lock
        override def put(elem: A): Unit =
          criticalSection:
            while isFull do _notFull.await()
            _buff(tail) = elem
            tail = next(tail)
            log(s"put $elem...")
            _notEmpty.signal()

        override def take(): A =
          criticalSection:
            while isEmpty do _notEmpty.await()
            val elem = _buff(head)
            head = next(head)
            log(s"take $elem...")
            _notFull.signal()
            elem


  object Producer:
    private val P_TIME = 1000
    private val counter = AtomicInteger(0)
    def apply(i: Int)(using bb: BoundedBuffer[Int]): Producer[Int] =
      new Producer[Int]:
        setName(s"Producer-$i")
        override def produce(): Int =
          work(P_TIME)
          counter.getAndIncrement()


  object Consumer:
    private val C_TIME = 5000
    def apply(i: Int)(using bb: BoundedBuffer[Int]): Consumer[Int] =
      new Consumer[Int]:
        setName(s"Consumer-$i")
        override def consume(elem: Int): Unit =
          work(C_TIME)
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
