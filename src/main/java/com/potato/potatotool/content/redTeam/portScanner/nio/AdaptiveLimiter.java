package com.potato.potatotool.content.redTeam.portScanner.nio;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;

public class AdaptiveLimiter {
    private static final long DEFAULT_FD_LIMIT = 1024L;
    private static final long WINDOWS_BATCH = 1500L;
    private static final long MIN_BATCH = 64L;
    private static final long MAX_BATCH = 5000L;

    /**
     * Estimate a safe initial batch size based on current OS file-descriptor limit.
     * Linux/macOS: clamp(fdLimit*0.7 - currentFD, MIN, MAX).
     * Windows: fixed WINDOWS_BATCH (Winsock SYN throttling).
     * Failure paths fall back to MIN_BATCH so we never amplify load on unknown systems.
     */
    public static int estimateInitialBatch() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return (int) WINDOWS_BATCH;
        }
        try {
            Object bean = ManagementFactory.getOperatingSystemMXBean();
            long limit = invokeLong(bean, "getMaxFileDescriptorCount", DEFAULT_FD_LIMIT);
            long open = invokeLong(bean, "getOpenFileDescriptorCount", 0L);
            long budget = (long) (limit * 0.7) - open;
            return (int) clamp(budget, MIN_BATCH, MAX_BATCH);
        } catch (Throwable ignored) {
            return (int) MIN_BATCH;
        }
    }

    private static long invokeLong(Object bean, String method, long fallback) {
        try {
            Method m = bean.getClass().getMethod(method);
            m.setAccessible(true);
            Object value = m.invoke(bean);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private static long clamp(long value, long min, long max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    private final int min;
    private final int max;
    private int current;
    private long lastAdjust;
    private long rttSum;
    private long samples;
    private long errors;

    public AdaptiveLimiter(int initial, int min, int max) {
        this.current = Math.max(min, Math.min(max, initial));
        this.min = min;
        this.max = max;
        this.lastAdjust = System.currentTimeMillis();
    }

    public synchronized int getCurrent() {
        return current;
    }

    public synchronized void recordSuccess(long rttMs) {
        rttSum += Math.max(0L, rttMs);
        samples++;
        adjustIfNeeded();
    }

    public synchronized void recordError() {
        errors++;
        adjustIfNeeded();
    }

    public synchronized void backoff() {
        current = Math.max(min, current / 2);
        lastAdjust = System.currentTimeMillis();
        resetWindow();
    }

    private void adjustIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastAdjust < 1000L) {
            return;
        }
        long avgRtt = samples == 0 ? 0 : rttSum / samples;
        if (errors > 0 || avgRtt > 1000L) {
            current = Math.max(min, current / 2);
        } else if (samples > 10 && avgRtt < 300L) {
            current = Math.min(max, current + Math.max(16, current / 10));
        }
        lastAdjust = now;
        resetWindow();
    }

    private void resetWindow() {
        rttSum = 0;
        samples = 0;
        errors = 0;
    }
}
