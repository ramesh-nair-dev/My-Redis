package com.example.miniredis.store;


import com.example.miniredis.strategy.LRUCachePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheStoreTest {

    private CacheStore<String> cacheStore;

    @BeforeEach
    void setup() {
        cacheStore = new CacheStore<>(3, new LRUCachePolicy<>());
    }

    @Test
    void testSetAndGet() {
        cacheStore.set("A", "Apple", 0);
        cacheStore.set("B", "Banana", 0);

        assertEquals("Apple", cacheStore.get("A"));
        assertEquals("Banana", cacheStore.get("B"));
        assertEquals(2, cacheStore.size());
    }

    @Test
    void testUpdateExistingKey() {
        cacheStore.set("A", "Apple", 0);
        cacheStore.set("A", "Avocado", 0);

        assertEquals("Avocado", cacheStore.get("A"));
        assertEquals(1, cacheStore.size());
    }

    @Test
    void testLRUEviction() {
        cacheStore.set("A", "Apple", 0);
        cacheStore.set("B", "Banana", 0);
        cacheStore.set("C", "Carrot", 0);

        // Access A so B becomes LRU
        cacheStore.get("A");

        cacheStore.set("D", "Dates", 0); // triggers eviction

        assertNull(cacheStore.get("B")); // B should be evicted
        assertNotNull(cacheStore.get("A"));
        assertNotNull(cacheStore.get("C"));
        assertNotNull(cacheStore.get("D"));
    }
}
