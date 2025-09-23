package com.example.miniredis.controller;


import com.example.miniredis.dtos.CacheRequest;
import com.example.miniredis.dtos.CacheResponse;
import com.example.miniredis.dtos.CacheStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CacheIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setup() {
        // Optional: clear any existing keys if your cache supports it
        restTemplate.delete("/cache/keys"); // implement delete-all in controller if needed
    }

    @Test
    void testFullCacheFlow() {
        // --- 1. SET ---
        CacheRequest<String> requestA = new CacheRequest<>();
        requestA.setKey("A");
        requestA.setValue("Apple");
        requestA.setTtl(0);

        CacheRequest<String> requestB = new CacheRequest<>();
        requestB.setKey("B");
        requestB.setValue("Banana");
        requestB.setTtl(0);

        ResponseEntity<String> setAResponse = restTemplate.postForEntity("/cache", requestA, String.class);
        ResponseEntity<String> setBResponse = restTemplate.postForEntity("/cache", requestB, String.class);

        assertEquals(HttpStatus.CREATED, setAResponse.getStatusCode());
        assertTrue(setAResponse.getBody().contains("A"));

        assertEquals(HttpStatus.CREATED, setBResponse.getStatusCode());
        assertTrue(setBResponse.getBody().contains("B"));

        // --- 2. GET ---
        ResponseEntity<CacheResponse<String>> getAResponse = restTemplate.exchange(
                "/cache/A",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CacheResponse<String>>() {}
        );
        assertEquals(HttpStatus.OK, getAResponse.getStatusCode());
        assertEquals("Apple", getAResponse.getBody().getValue());

        ResponseEntity<CacheResponse<String>> getBResponse = restTemplate.exchange(
                "/cache/B",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<CacheResponse<String>>() {}
        );
        assertEquals("Banana", getBResponse.getBody().getValue());

        // --- 3. LIST KEYS ---
        ResponseEntity<Set<String>> keysResponse = restTemplate.exchange(
                "/cache/keys",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Set<String>>() {}
        );
        Set<String> keys = keysResponse.getBody();
        assertTrue(keys.contains("A"));
        assertTrue(keys.contains("B"));

        // --- 4. STATS ---
        ResponseEntity<CacheStats> statsResponse = restTemplate.getForEntity("/cache/stats", CacheStats.class);
        CacheStats stats = statsResponse.getBody();
        assertNotNull(stats);
        assertEquals(2, stats.getCurrentSize()); // 2 keys added
        assertEquals(0, stats.getEvictions()); // nothing evicted yet

        // --- 5. DELETE ---
        restTemplate.delete("/cache/A");

        // --- 6. GET AFTER DELETE ---
        ResponseEntity<String> getAfterDelete = restTemplate.getForEntity("/cache/A", String.class);
        assertEquals(HttpStatus.NOT_FOUND, getAfterDelete.getStatusCode());

        // --- 7. FINAL STATS ---
        CacheStats finalStats = restTemplate.getForObject("/cache/stats", CacheStats.class);
        assertEquals(1, finalStats.getCurrentSize()); // only "B" remains
    }
}
