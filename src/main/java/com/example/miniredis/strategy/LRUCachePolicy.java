package com.example.miniredis.strategy;

import com.example.miniredis.models.Node;
import com.example.miniredis.store.CacheStore;

import java.util.logging.Logger;

/**
 * LRU eviction policy.
 * - Removes the least recently used node from CacheStore.
 * - Updates eviction counter atomically.
 */
public class LRUCachePolicy<T> implements EvictionPolicy<T> {

    private static final Logger LOGGER = Logger.getLogger(LRUCachePolicy.class.getName());

    @Override
    public void evict(CacheStore<T> cacheStore) {
        Node<T> lruNode = cacheStore.getTail(); // least recently used
        if (lruNode != null && lruNode.getKey() != null) {
            cacheStore.delete(lruNode.getKey()); // remove node from list & map
            cacheStore.incrementEvictionCounter(); // accurate eviction count
            LOGGER.info("Evicted LRU key: " + lruNode.getKey());
        }
    }
}
