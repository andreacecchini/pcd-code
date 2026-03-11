package it.unibo.semaphores;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

import static it.unibo.log.ThreadLogger.log;

public final class Mutex {
    private static final int PERMITS = 1;
    private static final int NCS_MS = 500;
    private static final int CS_MS = 5000;
    private final Semaphore semaphore = new Semaphore(PERMITS);
    private Thread owner = null;

    static void main() throws InterruptedException {
        final Mutex mutex = new Mutex();
        final Runnable task = () -> {
            while (true) {
                try {
                    log("Starting NCS...");
                    work(NCS_MS);
                    mutex.acquire();
                    try {
                        log("Entered CS. I have the lock.");
                        work(CS_MS);
                        log("Leaving CS...");
                    } finally {
                        mutex.release();
                    }
                    log("Back in NCS.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        final List<Thread> threads = Stream.of("P", "Q", "R").map(name -> new Thread(task, name)).toList();
        threads.forEach(Thread::start);
        for (Thread t : threads) {
            t.join();
        }
        System.out.println("Execution finished.");
    }

    public void acquire() throws InterruptedException {
        this.semaphore.acquire();
        this.owner = Thread.currentThread();
    }

    public void release() {
        if (this.owner != null && this.owner.equals(Thread.currentThread())) {
            this.semaphore.release();
        }
    }

    private static void work(int millis) throws InterruptedException {
        Thread.sleep(millis);
    }
}
