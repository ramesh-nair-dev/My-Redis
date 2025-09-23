package com.example.miniredis.controller;

import com.example.miniredis.dtos.CacheRequest;
import com.example.miniredis.dtos.CacheResponse;
import com.example.miniredis.dtos.CacheStats;
import com.example.miniredis.service.CacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CacheController.class)
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CacheService<String> cacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {}

    @Test
    void testSetKey() throws Exception {
        CacheRequest<String> request = new CacheRequest<>();
        request.setKey("A");
        request.setValue("Apple");
        request.setTtl(0);

        mockMvc.perform(post("/cache")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Key set successfully: A"));

        verify(cacheService, times(1)).set("A", "Apple", 0);
    }

    @Test
    void testGetKeyFound() throws Exception {
        when(cacheService.get("A")).thenReturn("Apple");

        mockMvc.perform(get("/cache/A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("A"))
                .andExpect(jsonPath("$.value").value("Apple"));

        verify(cacheService, times(1)).get("A");
    }

    @Test
    void testGetKeyNotFound() throws Exception {
        when(cacheService.get("B")).thenReturn(null);

        mockMvc.perform(get("/cache/B"))
                .andExpect(status().isNotFound());

        verify(cacheService, times(1)).get("B");
    }

    @Test
    void testDeleteKey() throws Exception {
        mockMvc.perform(delete("/cache/A"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deleted key: A"));

        verify(cacheService, times(1)).delete("A");
    }

    @Test
    void testListKeys() throws Exception {
        when(cacheService.listKeys()).thenReturn(Set.of("A", "B"));

        mockMvc.perform(get("/cache/keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("A"))
                .andExpect(jsonPath("$[1]").value("B"));

        verify(cacheService, times(1)).listKeys();
    }

    @Test
    void testStats() throws Exception {
        Map<String, Object> stats = Map.of(
                "maxCapacity", 3,
                "currentSize", 2,
                "hits", 1L,
                "misses", 1L,
                "evictions", 0L
        );

        when(cacheService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/cache/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCapacity").value(3))
                .andExpect(jsonPath("$.currentSize").value(2))
                .andExpect(jsonPath("$.hits").value(1))
                .andExpect(jsonPath("$.misses").value(1))
                .andExpect(jsonPath("$.evictions").value(0));

        verify(cacheService, times(1)).getStats();
    }
}

