package org.codrshi.metric;

public final class Timer {
    private Timer() {}

    public static long start() {
        return System.nanoTime();
    }

    public static long stop(long startNanos) {
        return System.nanoTime() - startNanos;
    }
}
