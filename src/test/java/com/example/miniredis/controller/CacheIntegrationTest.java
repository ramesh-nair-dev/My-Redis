package com.example.miniredis.controller;

import com.example.miniredis.service.CacheService;
import com.example.miniredis.store.CacheStore;
import com.example.miniredis.strategy.LFUEvictionPolicy;
import com.example.miniredis.strategy.LRUCachePolicy;
import com.example.miniredis.persistence.PersistenceManager;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CacheIntegrationTest {

    private CacheService<String, String> cacheService;
    private PersistenceManager<String, String> persistenceManager;

    @BeforeEach
    void setUp() {
        // Mock persistence manager
        persistenceManager = Mockito.mock(PersistenceManager.class);
        Mockito.when(persistenceManager.load()).thenReturn(new ConcurrentHashMap<>());

        // Create cache with maxCapacity=3, LRU policy
        CacheStore<String, String> store = new CacheStore<>(
                3,
                new LRUCachePolicy<>(),
                persistenceManager
        );

        cacheService = new CacheService<>(store);
    }

    @Test
    @Order(1)
    void testSetAndGet() {
        cacheService.set("A", "ValueA", 0);
        String value = cacheService.get("A");
        assertEquals("ValueA", value, "Cache get should return the correct value");
    }

    @Test
    @Order(2)
    void testDelete() {
        cacheService.set("B", "ValueB", 0);
        cacheService.delete("B");
        assertNull(cacheService.get("B"), "Deleted key should return null");
    }

    @Test
    @Order(3)
    void testTTLExpiration() throws InterruptedException {
        cacheService.set("C", "ValueC", 500); // 500 ms TTL
        Thread.sleep(600);
        assertNull(cacheService.get("C"), "Key should expire after TTL");
    }

    @Test
    @Order(4)
    void testLRUEviction() {
        cacheService.set("X", "ValueX", 0);
        cacheService.set("Y", "ValueY", 0);
        cacheService.set("Z", "ValueZ", 0);
        cacheService.set("W", "ValueW", 0); // Should evict "X" (oldest, LRU)

        Set<String> keys = cacheService.listKeys();
        assertFalse(keys.contains("X"), "LRU eviction should remove oldest key");
        assertTrue(keys.containsAll(Set.of("Y", "Z", "W")));
    }

    @Test
    @Order(5)
    void testPersistenceCall() throws InterruptedException {
        cacheService.set("P1", "Persist1", 0);
        // Wait for async save
        Thread.sleep(200);

        // Verify persistenceManager.save() is called
        Mockito.verify(persistenceManager, Mockito.atLeastOnce()).save(Mockito.any(Map.class));
    }

    @Test
    @Order(6)
    void testLFUEviction() {
        // Create new store with LFU policy
        CacheStore<String, String> storeLFU = new CacheStore<>(3, new LFUEvictionPolicy<>(), null);
        CacheService<String, String> cacheLFU = new CacheService<>(storeLFU);

        // Add keys and access frequencies
        cacheLFU.set("A", "1", 0); // freq 1
        cacheLFU.set("B", "2", 0); // freq 1
        cacheLFU.set("C", "3", 0); // freq 1

        cacheLFU.get("A"); // freq A=2
        cacheLFU.get("A"); // freq A=3
        cacheLFU.get("B"); // freq B=2

        cacheLFU.set("D", "4", 0); // Should evict C (freq 1)

        Set<String> keys = cacheLFU.listKeys();
        assertFalse(keys.contains("C"), "LFU eviction should remove least frequently used key");
        assertTrue(keys.containsAll(Set.of("A", "B", "D")));
    }
}
