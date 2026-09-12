package com.thanh.autotrade.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple cache utility với TTL (time-to-live)
 */
public class CacheUtil {
    public static class CacheEntry<T> {
        T value;
        long expiryTime;

        CacheEntry(T value, long ttlMillis) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private static final Map<String, CacheEntry<?>> cache = new HashMap<>();

    public static <T> void put(String key, T value, long ttlMillis) {
        cache.put(key, new CacheEntry<>(value, ttlMillis));
    }

    public static <T> T get(String key, Class<T> type) {
        CacheEntry<?> entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return type.cast(entry.value);
    }

    public static void invalidate(String key) {
        cache.remove(key);
    }

    public static void clear() {
        cache.clear();
    }
}
