package com.example.miniredis.store;

import com.example.miniredis.models.CacheValue;
import com.example.miniredis.persistence.PersistenceManager;
import com.example.miniredis.strategy.EvictionPolicy;
import lombok.Getter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Logger;

@Getter
public class CacheStore<K, V> {

    private static final Logger logger = Logger.getLogger(CacheStore.class.getName());

    // store CacheValue wrappers
    private final Map<K, CacheValue<V>> cache = new ConcurrentHashMap<>();
    private final int maxCapacity;
    private final EvictionPolicy<K> evictionPolicy;
    private final PersistenceManager<K, V> persistenceManager;

    // Executors
    private final ScheduledExecutorService ttlExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cache-ttl-cleaner");
        t.setDaemon(true);
        return t;
    });
    private final ExecutorService persistExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cache-persist-worker");
        t.setDaemon(true);
        return t;
    });

    public CacheStore(int maxCapacity,
                      EvictionPolicy<K> evictionPolicy,
                      PersistenceManager<K, V> persistenceManager) {
        this.maxCapacity = maxCapacity;
        this.evictionPolicy = evictionPolicy;
        this.persistenceManager = persistenceManager;

        // load persisted snapshot if available
        if (persistenceManager != null) {
            Map<K, V> loaded = persistenceManager.load();
            if (loaded != null && !loaded.isEmpty()) {
                loaded.forEach((k, v) -> {
                    cache.put(k, new CacheValue<>(v, 0L)); // persisted entries default to no TTL
                    evictionPolicy.keyAdded(k);
                });
            }
        }

        // schedule TTL cleanup every second
        ttlExecutor.scheduleAtFixedRate(this::cleanExpired, 1, 1, TimeUnit.SECONDS);
    }

    public void set(K key, V value, long ttlMillis) {
        if (key == null) throw new IllegalArgumentException("key cannot be null");
        synchronized (this) {
            if (!cache.containsKey(key) && cache.size() >= maxCapacity) {
                K evict = evictionPolicy.evictKey();
                if (evict != null) {
                    cache.remove(evict);
                    evictionPolicy.keyRemoved(evict);
                    logger.info(() -> "Evicted key: " + evict + " by policy=" + evictionPolicy.name());
                }
            }
            cache.put(key, new CacheValue<>(value, ttlMillis));
            evictionPolicy.keyAdded(key);
            saveAsync();
            logger.fine(() -> "SET key=" + key + " ttl=" + ttlMillis);
        }
    }

    public V get(K key) {
        if (key == null) return null;
        synchronized (this) {
            CacheValue<V> wrapper = cache.get(key);
            if (wrapper == null) {
                logger.fine(() -> "GET miss: " + key);
                return null;
            }
            // choose absolute expiry; change to isExpiredSliding() for sliding TTL
            if (wrapper.isExpired()) {
                // expire and count as miss
                cache.remove(key);
                evictionPolicy.keyRemoved(key);
                saveAsync();
                logger.fine(() -> "GET miss (expired): " + key);
                return null;
            }
            evictionPolicy.keyAccessed(key);
            logger.fine(() -> "GET hit: " + key);
            return wrapper.getValue(); // updates lastAccessTime
        }
    }

    public void delete(K key) {
        if (key == null) return;
        synchronized (this) {
            if (cache.remove(key) != null) {
                evictionPolicy.keyRemoved(key);
                saveAsync();
                logger.fine(() -> "DELETE key: " + key);
            }
        }
    }

    public Set<K> listKeys() {
        return cache.keySet();
    }

    private void cleanExpired() {
        try {
            for (K key : cache.keySet()) {
                CacheValue<V> wrapper = cache.get(key);
                if (wrapper != null && wrapper.isExpired()) {
                    delete(key);
                }
            }
        } catch (Exception e) {
            logger.warning("Exception during TTL cleanup: " + e.getMessage());
        }
    }

//    private void saveAsync() {
//        if (persistenceManager == null) return;
//        Map<K, V> snapshot = new ConcurrentHashMap<>();
//        cache.forEach((k, wrapper) -> snapshot.put(k, wrapper.getValue()));
//        persistExecutor.submit(() -> {
//            try {
//                persistenceManager.save(snapshot);
//            } catch (Exception e) {
//                logger.warning("Persistence save failed: " + e.getMessage());
//            }
//        });
//    }

    public void saveAsync() {
        if (persistenceManager != null) {
            Map<K, V> snapshot = new ConcurrentHashMap<>();
            cache.forEach((k, wrapper) -> snapshot.put(k, wrapper.getValue()));
            persistenceManager.save(snapshot);
        }
    }


    public void shutdown() {
        ttlExecutor.shutdownNow();
        persistExecutor.shutdownNow();
    }
}
