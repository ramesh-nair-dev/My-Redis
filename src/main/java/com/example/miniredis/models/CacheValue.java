package com.example.miniredis.models;

import lombok.Getter;


@Getter
public class CacheValue <T>{
    private final T value;
    private final long creationTime;
    private final long ttl;
    private long lastAccessTime;

    public CacheValue(T value, long ttl) {
        this.value = value;
        this.creationTime = System.currentTimeMillis();
        this.ttl = ttl;
        this.lastAccessTime = creationTime;
    }

    public T getValue() {
        this.lastAccessTime = System.currentTimeMillis();
        return value;
    }

    public boolean isExpired() {
        return ttl > 0 && (System.currentTimeMillis() - creationTime) >= ttl;
    }

}
