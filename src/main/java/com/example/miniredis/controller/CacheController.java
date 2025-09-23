package com.example.miniredis.controller;

import com.example.miniredis.dtos.CacheRequest;
import com.example.miniredis.dtos.CacheResponse;
import com.example.miniredis.service.CacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/cache")
public class CacheController<V> {

    private final CacheService<String, V> cacheService;

    public CacheController(CacheService<String, V> cacheService) {
        this.cacheService = cacheService;
    }

    @PostMapping
    public ResponseEntity<String> set(@RequestBody CacheRequest<String, V> request) {
        cacheService.set(request.getKey(), request.getValue(), request.getTtl());
        return ResponseEntity.status(HttpStatus.CREATED).body("Key set successfully: " + request.getKey());
    }

    @GetMapping("/{key}")
    public ResponseEntity<CacheResponse<String, V>> get(@PathVariable String key) {
        V value = cacheService.get(key);
        if (value == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new CacheResponse<>(key, value));
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<String> delete(@PathVariable String key) {
        cacheService.delete(key);
        return ResponseEntity.ok("Deleted key: " + key);
    }

    @GetMapping("/keys")
    public ResponseEntity<Set<String>> listKeys() {
        return ResponseEntity.ok(cacheService.listKeys());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(cacheService.getStats());
    }
}
