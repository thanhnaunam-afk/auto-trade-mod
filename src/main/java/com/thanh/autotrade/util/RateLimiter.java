package com.thanh.autotrade.util;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple rate limiter - throttle actions
 */
public class RateLimiter {
    private final long timeWindowMillis;
    private final int maxRequests;
    private final Queue<Long> requestTimestamps;

    public RateLimiter(long timeWindowMillis, int maxRequests) {
        this.timeWindowMillis = timeWindowMillis;
        this.maxRequests = maxRequests;
        this.requestTimestamps = new ConcurrentLinkedQueue<>();
    }

    public boolean tryConsume() {
        long now = System.currentTimeMillis();
        long windowStart = now - timeWindowMillis;

        // Remove old timestamps outside window
        while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < windowStart) {
            requestTimestamps.poll();
        }

        // Check if we can consume
        if (requestTimestamps.size() < maxRequests) {
            requestTimestamps.offer(now);
            return true;
        }
        return false;
    }

    public int getAvailable() {
        long now = System.currentTimeMillis();
        long windowStart = now - timeWindowMillis;

        while (!requestTimestamps.isEmpty() && requestTimestamps.peek() < windowStart) {
            requestTimestamps.poll();
        }

        return maxRequests - requestTimestamps.size();
    }
}
