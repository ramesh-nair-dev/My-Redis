package com.example.miniredis.persistence;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory persistence (useful for tests). Not durable across JVM restarts.
 * Replace with file/DB-backed implementation for real persistence.
 */
public class InMemoryPersistenceManager<K, V> implements PersistenceManager<K, V> {

    private final Map<K, V> storage = new ConcurrentHashMap<>();

    @Override
    public void save(Map<K, V> snapshot) {
        storage.clear();
        storage.putAll(snapshot);
    }

    @Override
    public Map<K, V> load() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(storage));
    }
}
