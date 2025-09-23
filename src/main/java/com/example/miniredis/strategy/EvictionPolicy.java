package com.example.miniredis.strategy;

public interface EvictionPolicy<K> {
    void keyAdded(K key);       // called after a key is added
    void keyAccessed(K key);    // called on GET (or accesses that should affect policy)
    void keyRemoved(K key);     // called when key deleted/evicted/expired
    K evictKey();               // decide which key to evict (may remove internal structures)
    String name();
}
