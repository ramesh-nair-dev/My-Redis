package com.example.miniredis.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CacheStats {
    private long hits;
    private long misses;
    private long evictions;
    private int maxCapacity;
    private int currentSize;

    public static CacheStats toCacheStats(Map<String , Object> stats) {
        CacheStats cacheStats = new CacheStats();
        cacheStats.setHits((Long) stats.get("hits"));
        cacheStats.setMisses((Long) stats.get("misses"));
        cacheStats.setEvictions((Long) stats.get("evictions"));
        cacheStats.setMaxCapacity((Integer) stats.get("maxCapacity"));
        cacheStats.setCurrentSize((Integer) stats.get("currentSize"));
        return cacheStats;
    }

}
