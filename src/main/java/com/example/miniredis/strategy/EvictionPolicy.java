package com.example.miniredis.strategy;

import com.example.miniredis.store.CacheStore;

public interface EvictionPolicy<T> {
    void evict(CacheStore<T> cacheStore);
}
