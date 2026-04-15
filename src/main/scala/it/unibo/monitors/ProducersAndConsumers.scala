package it.unibo.monitors

import it.unibo.log.ThreadLogger.*
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable

object ProducersAndConsumers:

  object Utils:
    def loop(body: => Unit): Unit = while (true) {
      body
    }

  import Utils.*

  class BoundedBuffer[A](capacity: Int):
    private val queue = mutable.Queue[A]()

    def put(elem: A): Unit = synchronized {
      while (queue.size == capacity) {
        wait()
      }
      queue.enqueue(elem)
      log(s"enqueue $elem...")
      notifyAll()
    }

    def take(): A = synchronized {
      while (queue.isEmpty) {
        wait()
      }
      val elem = queue.dequeue()
      log(s"dequeue $elem...")
      notifyAll()
      elem
    }

  end BoundedBuffer

  trait Producer[A](bb: BoundedBuffer[A]) extends Thread:

    def next: A
    override def run(): Unit =
      loop:
        val elem: A = next
        bb.put(elem)

  object Producer:
    private val producingTime = 1000
    private val counter = AtomicInteger(0)
    def apply(i: Int)(bb: BoundedBuffer[Int]): Producer[Int] = new Producer[Int](bb):
      setName(s"Producer-$i")
      override def next: Int = {
        Thread.sleep(producingTime)
        counter.getAndIncrement()
      }

  trait Consumer[A](bb: BoundedBuffer[A]) extends Thread:

    def consume(elem: A): Unit
    override def run(): Unit =
      loop:
        val elem: A = bb.take()
        consume(elem)

  object Consumer:
    private val consumingTime = 5000
    def apply(i: Int)(bb: BoundedBuffer[Int]): Consumer[Int] = new Consumer[Int](bb):
      setName(s"Consumer-$i")
      override def consume(elem: Int): Unit =
        Thread.sleep(consumingTime);
        log(elem.toString)


end ProducersAndConsumers

@main def testProducersAndConsumers(): Unit =
  import ProducersAndConsumers.*
  val nProducers = 2
  val nConsumers = 5
  val capacity = 10
  val bb = BoundedBuffer[Int](capacity)
  for i <- 1 to nProducers do Producer(i)(bb).start()
  for i <- 1 to nConsumers do Consumer(i)(bb).start()
