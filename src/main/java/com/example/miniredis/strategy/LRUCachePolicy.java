package com.example.miniredis.strategy;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCachePolicy<K> implements EvictionPolicy<K> {

    // LinkedHashMap with accessOrder=true moves accessed keys to end
    private final Map<K, Boolean> order = new LinkedHashMap<>(16, 0.75f, true);

    @Override
    public synchronized void keyAdded(K key) {
        order.put(key, Boolean.TRUE);
    }

    @Override
    public synchronized void keyAccessed(K key) {
        // with accessOrder=true, a read through map will reorder;
        // ensure the map sees this access by doing get/put if necessary
        if (order.containsKey(key)) {
            // re-put to update order (cheap)
            order.remove(key);
            order.put(key, Boolean.TRUE);
        }
    }

    @Override
    public synchronized void keyRemoved(K key) {
        order.remove(key);
    }

    @Override
    public synchronized K evictKey() {
        if (order.isEmpty()) return null;
        K oldest = order.keySet().iterator().next();
        order.remove(oldest);
        return oldest;
    }

    @Override
    public String name() {
        return "LRU";
    }
}
