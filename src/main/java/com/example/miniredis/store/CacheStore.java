package com.example.miniredis.store;

import com.example.miniredis.models.CacheValue;
import com.example.miniredis.models.Node;
import com.example.miniredis.strategy.EvictionPolicy;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Production-grade thread-safe CacheStore
 * - Doubly-linked list + HashMap (O(1) LRU)
 * - TTL support
 * - Hits / Misses / Evictions counters
 * - Minimal locking and atomic stats
 * - Detailed logging
 */
public class CacheStore<T> {

    private static final Logger LOGGER = Logger.getLogger(CacheStore.class.getName());

    private final Map<String, Node<T>> map;
    @Getter
    private final int maxCapacity;
    private final EvictionPolicy<T> evictionPolicy;

    private final Node<T> head; // dummy head
    private final Node<T> tail; // dummy tail

    private final ReentrantLock lock = new ReentrantLock();

    // Counters
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);

    public CacheStore(int maxCapacity, EvictionPolicy<T> evictionPolicy) {
        this.map = new ConcurrentHashMap<>();
        this.maxCapacity = maxCapacity;
        this.evictionPolicy = evictionPolicy;

        head = new Node<>(null, null);
        tail = new Node<>(null, null);
        head.setNext(tail);
        tail.setPrev(head);
    }

    // ---------------- Core Operations ----------------

    public void set(String key, T value, long ttl) {
        lock.lock(); // single lock for list modifications
        try {
            Node<T> node = map.get(key);

            if (node != null) {
                // Key exists → update value and move to head
                node.setValue(new CacheValue<>(value, ttl));
                moveToHead(node);
                LOGGER.info("SET key updated: " + key);
            } else {
                if (map.size() >= maxCapacity) {
                    evictionPolicy.evict(this); // actual eviction increments evictions
                }
                Node<T> newNode = new Node<>(key, new CacheValue<>(value, ttl));
                addNodeAtHead(newNode);
                map.put(key, newNode);
                LOGGER.info("SET new key: " + key);
            }
        } finally {
            lock.unlock();
        }
    }

    public T get(String key) {
        Node<T> node = map.get(key);
        if (node == null) {
            misses.incrementAndGet();
            LOGGER.info("GET miss: " + key);
            return null;
        }

        lock.lock();
        try {
            if (node.getValue().isExpired()) {
                removeNode(node);
                map.remove(key);
                misses.incrementAndGet();
                LOGGER.info("GET expired key removed: " + key);
                return null;
            }
            moveToHead(node);
            hits.incrementAndGet();
            LOGGER.info("GET hit: " + key);
            return node.getValue().getValue();
        } finally {
            lock.unlock();
        }
    }

    public void delete(String key) {
        lock.lock();
        try {
            Node<T> node = map.get(key);
            if (node != null) {
                removeNode(node);
                map.remove(key);
                LOGGER.info("DELETE key: " + key);
            } else {
                LOGGER.info("DELETE key not found: " + key);
            }
        } finally {
            lock.unlock();
        }
    }

    // ---------------- Counters ----------------

    public long getHits() { return hits.get(); }
    public long getMisses() { return misses.get(); }
    public long getEvictions() { return evictions.get(); }
    public int size() { return map.size(); }

    // ---------------- Doubly-Linked List Helpers ----------------

    private void addNodeAtHead(Node<T> node) {
        node.setPrev(head);
        node.setNext(head.getNext());
        head.getNext().setPrev(node);
        head.setNext(node);
    }

    private void removeNode(Node<T> node) {
        Node<T> prev = node.getPrev();
        Node<T> next = node.getNext();

        if (prev != null) prev.setNext(next);
        if (next != null) next.setPrev(prev);

        node.setPrev(null);
        node.setNext(null);
    }

    private void moveToHead(Node<T> node) {
        removeNode(node);
        addNodeAtHead(node);
    }

    // ---------------- TTL Cleanup ----------------

    public void cleanUpExpiredKeys() {
        for (String key : map.keySet()) {
            Node<T> node = map.get(key);
            if (node != null && node.getValue().isExpired()) {
                lock.lock();
                try {
                    removeNode(node);
                    map.remove(key);
                    misses.incrementAndGet();
                    LOGGER.info("Expired key removed during cleanup: " + key);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    // ---------------- Accessors for Eviction ----------------

    public Node<T> getTail() {
        lock.lock();
        try {
            return tail.getPrev();
        } finally {
            lock.unlock();
        }
    }

    public Map<String, Node<T>> getSnapshot() {
        return Map.copyOf(map);
    }

    public void incrementEvictionCounter() {
        evictions.incrementAndGet();
    }
}
