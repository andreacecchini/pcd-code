package it.unibo.log;

public class ThreadLogger {
    public static void log(String message) {
        String threadMessage = "[" + Thread.currentThread().getName() + "] " + message;
        System.out.println(threadMessage);
    }
}
