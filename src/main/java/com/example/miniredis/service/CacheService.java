package com.example.miniredis.service;

import com.example.miniredis.store.CacheStore;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@Service
public class CacheService<K, V> {

    private static final Logger logger = Logger.getLogger(CacheService.class.getName());

    private final CacheStore<K, V> cacheStore;

    // constructor injection (CacheStore bean provided via configuration)
    public CacheService(CacheStore<K, V> cacheStore) {
        this.cacheStore = cacheStore;
    }

    public void set(K key, V value, long ttlMillis) {
        if (key == null) throw new IllegalArgumentException("key cannot be null");
        if (ttlMillis < 0) throw new IllegalArgumentException("ttlMillis cannot be negative");
        cacheStore.set(key, value, ttlMillis);
        logger.fine(() -> "Service: set key=" + key);
    }

    public V get(K key) {
        if (key == null) return null;
        return cacheStore.get(key);
    }

    public void delete(K key) {
        if (key == null) return;
        cacheStore.delete(key);
    }

    public Set<K> listKeys() {
        return cacheStore.listKeys();
    }

    public Map<String, Object> getStats() {
        return Map.of(
                "maxCapacity", cacheStore.getMaxCapacity(),
                "currentSize", cacheStore.getCache().size(),
                "evictionPolicy", cacheStore.getEvictionPolicy().name()
        );
    }
}
