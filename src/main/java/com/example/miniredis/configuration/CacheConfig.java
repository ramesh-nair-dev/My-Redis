package com.example.miniredis.configuration;

import com.example.miniredis.store.CacheStore;
import com.example.miniredis.strategy.LRUCachePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheStore<String> cacheStore() {
        // max capacity 3, LRU eviction policy
        return new CacheStore<>(3, new LRUCachePolicy<>());
    }
}
