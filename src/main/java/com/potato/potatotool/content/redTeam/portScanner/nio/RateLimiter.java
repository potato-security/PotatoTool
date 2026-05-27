package com.potato.potatotool.content.redTeam.portScanner.nio;

public class RateLimiter {
    private final int permitsPerSecond;
    private long nextAllowedNanos;

    public RateLimiter(int permitsPerSecond) {
        this.permitsPerSecond = Math.max(1, permitsPerSecond);
        this.nextAllowedNanos = System.nanoTime();
    }

    public void acquire() {
        long interval = 1000000000L / permitsPerSecond;
        synchronized (this) {
            long now = System.nanoTime();
            if (nextAllowedNanos > now) {
                sleepNanos(nextAllowedNanos - now);
                now = System.nanoTime();
            }
            nextAllowedNanos = Math.max(now, nextAllowedNanos) + interval;
        }
    }

    private void sleepNanos(long nanos) {
        long millis = nanos / 1000000L;
        int extra = (int) (nanos % 1000000L);
        try {
            Thread.sleep(millis, extra);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
