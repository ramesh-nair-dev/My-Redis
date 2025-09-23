package com.example.miniredis.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheRequest<K, V> {
    private K key;
    private V value;
    private long ttl; // in milliseconds
}