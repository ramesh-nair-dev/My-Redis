package com.example.miniredis.service;
import com.example.miniredis.store.CacheStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class CacheService<T> {

    private final CacheStore<T> cacheStore;

    public CacheService(CacheStore<T> cacheStore) {
        this.cacheStore = cacheStore;
    }

    public void set(String key, T value, long ttl) {
        cacheStore.set(key, value, ttl);
    }

    public T get(String key) {
        return cacheStore.get(key);
    }

    public void delete(String key) {
        cacheStore.delete(key);
    }

    public Set<String> listKeys() {
        return cacheStore.getSnapshot().keySet();
    }

    public Map<String, Object> getStats() {
        return Map.of(
                "maxCapacity", cacheStore.getMaxCapacity(),
                "currentSize", cacheStore.getSnapshot().size(),
                "hits", cacheStore.getHits(),
                "misses", cacheStore.getMisses(),
                "evictions", cacheStore.getEvictions()
        );
    }
}
