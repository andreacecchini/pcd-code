package it.unibo.semaphores;


import java.util.List;
import java.util.stream.IntStream;

import static it.unibo.log.ThreadLogger.log;

public class SafeCounter {
    private int count = 0;
    private final Mutex mutex = new Mutex();

    public int getCount() throws InterruptedException {
        try {
            this.mutex.acquire();
            // CS
            return this.count;
        } finally {
            this.mutex.release();
        }
    }

    public void inc() {
        try {
            this.mutex.acquire();
            // CS
            this.count++;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            this.mutex.release();
        }
    }

    static void main() throws InterruptedException {
        final int numThreads = Runtime.getRuntime().availableProcessors();
        final int incrementsPerThread = 10_000;
        final int expectedCount = numThreads * incrementsPerThread;
        final SafeCounter counter = new SafeCounter();
        final Runnable task = () -> IntStream.range(0, incrementsPerThread).forEach(_ -> counter.inc());
        final List<Thread> threads = IntStream.range(0, numThreads).mapToObj(i -> new Thread(task, "Thread-" + i)).toList();
        threads.forEach(Thread::start);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log("Count: " + counter.getCount());
        log("Expected Count: " + expectedCount);
    }
}
