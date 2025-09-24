package com.example.miniredis.controller;
import com.example.miniredis.models.CacheValue;
import com.example.miniredis.persistence.PersistenceManager;
import com.example.miniredis.service.CacheService;
import com.example.miniredis.store.CacheStore;
import com.example.miniredis.strategy.LRUCachePolicy;
import com.example.miniredis.strategy.LFUEvictionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class CacheFullIntegrationTest {

    private CacheService<String, String> lruCacheService;
    private CacheService<String, String> lfuCacheService;
    private InMemoryPersistenceManager<String, String> persistenceManager;

    @BeforeEach
    void setup() {
        // Simulated persistence
        persistenceManager = new InMemoryPersistenceManager<>();



        // LRU cache with max capacity 3 and persistence
        CacheStore<String, String> lruStore = new CacheStore<>(3, new LRUCachePolicy<>(), persistenceManager);
        lruCacheService = new CacheService<>(lruStore);

        // LFU cache with max capacity 3 and persistence
        CacheStore<String, String> lfuStore = new CacheStore<>(3, new LFUEvictionPolicy<>(), persistenceManager);
        lfuCacheService = new CacheService<>(lfuStore);
    }

    @Test
    void testBasicSetGetDelete() {
        lruCacheService.set("A", "Apple", 0);
        assertThat(lruCacheService.get("A")).isEqualTo("Apple");

        lruCacheService.delete("A");
        assertThat(lruCacheService.get("A")).isNull();
    }

    @Test
    void testTTLExpiration() throws InterruptedException {
        lruCacheService.set("B", "Banana", 500); // TTL = 0.5s
        assertThat(lruCacheService.get("B")).isEqualTo("Banana");

        Thread.sleep(600);
        assertThat(lruCacheService.get("B")).isNull(); // expired
    }

    @Test
    void testLRUEviction() {
        lruCacheService.set("A", "Apple", 0);
        lruCacheService.set("B", "Banana", 0);
        lruCacheService.set("C", "Cat", 0);

        lruCacheService.get("A");
        lruCacheService.get("B");

        lruCacheService.set("D", "Dog", 0); // should evict C

        Set<String> keys = lruCacheService.listKeys();
        assertThat(keys).containsExactlyInAnyOrder("A", "B", "D");
        assertThat(lruCacheService.get("C")).isNull();
    }

    @Test
    void testLFUEviction() {
        lfuCacheService.set("X", "Xylophone", 0);
        lfuCacheService.set("Y", "Yak", 0);
        lfuCacheService.set("Z", "Zebra", 0);

        lfuCacheService.get("X");
        lfuCacheService.get("X");
        lfuCacheService.get("Y");

        lfuCacheService.set("W", "Wolf", 0); // should evict Z

        Set<String> keys = lfuCacheService.listKeys();
        assertThat(keys).containsExactlyInAnyOrder("X", "Y", "W");
        assertThat(lfuCacheService.get("Z")).isNull();
    }

    @Test
    void testPersistenceSimulation() {
        lruCacheService.set("P1", "Persist1", 0);
        lruCacheService.set("P2", "Persist2", 0);

        Map<String, String> persisted = persistenceManager.load();
        assertThat(persisted).containsEntry("P1", "Persist1")
                             .containsEntry("P2", "Persist2");
    }

    /**
     * Simple in-memory persistence manager to simulate DB/file.
     */
    static class InMemoryPersistenceManager<K, V> implements PersistenceManager<K, V> {
        private final Map<K, V> storage = new ConcurrentHashMap<>();

        @Override
        public void save(Map<K, V> cacheSnapshot) {
            storage.clear();
            storage.putAll(cacheSnapshot);
        }

        @Override
        public Map<K, V> load() {
            return new ConcurrentHashMap<>(storage);
        }
    }
}
