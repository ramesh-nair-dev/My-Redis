package com.example.miniredis.dtos;

import lombok.Data;

@Data
public class CacheRequest<T> {
    private String key;
    private T value;
    private long ttl; // optional, default 0 = infinite
}
