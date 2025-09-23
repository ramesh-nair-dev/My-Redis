package com.example.miniredis.models;

import lombok.Getter;

@Getter
public class CacheValue<T> {
    private final T value;
    private final long creationTime;
    private final long ttlMillis; // 0 => never expires
    private volatile long lastAccessTime;

    public CacheValue(T value, long ttlMillis) {
        this.value = value;
        this.creationTime = System.currentTimeMillis();
        this.ttlMillis = ttlMillis;
        this.lastAccessTime = this.creationTime;
    }

    /**
     * Return value and update lastAccessTime (for LRU / sliding TTL).
     */
    public T getValue() {
        this.lastAccessTime = System.currentTimeMillis();
        return value;
    }

    /**
     * Absolute expiry based on creation time.
     */
    public boolean isExpired() {
        return ttlMillis > 0 && (System.currentTimeMillis() - creationTime) >= ttlMillis;
    }

    /**
     * Sliding expiry: expiry measured against last access.
     */
    public boolean isExpiredSliding() {
        return ttlMillis > 0 && (System.currentTimeMillis() - lastAccessTime) >= ttlMillis;
    }

    public long getExpiryTime() {
        return ttlMillis > 0 ? creationTime + ttlMillis : Long.MAX_VALUE;
    }
}
