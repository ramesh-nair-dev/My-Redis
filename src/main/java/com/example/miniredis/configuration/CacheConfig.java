package com.example.miniredis.config;

import com.example.miniredis.persistence.InMemoryPersistenceManager;
import com.example.miniredis.persistence.PersistenceManager;
import com.example.miniredis.store.CacheStore;
import com.example.miniredis.strategy.EvictionPolicy;
import com.example.miniredis.strategy.LRUCachePolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public EvictionPolicy<String> evictionPolicy() {
        return new LRUCachePolicy<>(); // swap for LFUCachePolicy<>()
    }

    @Bean
    public PersistenceManager<String, Object> persistenceManager() {
        // PoC in-memory persistence. Replace with file/db implementation for production.
        return new InMemoryPersistenceManager<>();
    }

    @Bean
    public CacheStore<String, Object> cacheStore(EvictionPolicy<String> evictionPolicy,
                                                 PersistenceManager<String, Object> persistenceManager) {
        // 100 default capacity — tune as needed or expose as config property
        return new CacheStore<>(100, evictionPolicy, persistenceManager);
    }
}
