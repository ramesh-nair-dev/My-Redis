package com.example.miniredis.persistence;

import java.util.Map;

public interface PersistenceManager<K, V> {
    void save(Map<K, V> snapshot);
    Map<K, V> load();
}
