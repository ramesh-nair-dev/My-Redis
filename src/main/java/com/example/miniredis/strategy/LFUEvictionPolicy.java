package com.example.miniredis.strategy;

import java.util.*;

public class LFUEvictionPolicy<K> implements EvictionPolicy<K> {

    // key -> frequency
    private final Map<K, Integer> keyFreq = new HashMap<>();
    // freq -> keys with that frequency (ordered by insertion)
    private final Map<Integer, LinkedHashSet<K>> freqMap = new HashMap<>();
    private int minFreq = 1;

    @Override
    public synchronized void keyAdded(K key) {
        keyFreq.put(key, 1);
        freqMap.computeIfAbsent(1, f -> new LinkedHashSet<>()).add(key);
        minFreq = 1;
    }

    @Override
    public synchronized void keyAccessed(K key) {
        Integer freq = keyFreq.get(key);
        if (freq == null) return; // key not tracked (maybe removed concurrently)
        LinkedHashSet<K> set = freqMap.get(freq);
        if (set != null) set.remove(key);
        if (set == null || set.isEmpty()) {
            freqMap.remove(freq);
            if (freq == minFreq) minFreq++;
        }
        int newFreq = freq + 1;
        keyFreq.put(key, newFreq);
        freqMap.computeIfAbsent(newFreq, f -> new LinkedHashSet<>()).add(key);
    }

    @Override
    public synchronized void keyRemoved(K key) {
        Integer freq = keyFreq.remove(key);
        if (freq != null) {
            LinkedHashSet<K> set = freqMap.get(freq);
            if (set != null) {
                set.remove(key);
                if (set.isEmpty()) freqMap.remove(freq);
            }
            // recompute minFreq if needed (simple way)
            if (freq == minFreq && !freqMap.containsKey(minFreq)) {
                minFreq = freqMap.keySet().stream().min(Integer::compareTo).orElse(1);
            }
        }
    }

    @Override
    public synchronized K evictKey() {
        LinkedHashSet<K> set = freqMap.get(minFreq);
        if (set == null || set.isEmpty()) return null;
        Iterator<K> it = set.iterator();
        K evict = it.next();
        it.remove();
        keyFreq.remove(evict);
        if (set.isEmpty()) freqMap.remove(minFreq);
        return evict;
    }

    @Override
    public String name() {
        return "LFU";
    }
}
