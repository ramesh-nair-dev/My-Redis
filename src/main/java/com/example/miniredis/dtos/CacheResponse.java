package com.example.miniredis.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CacheResponse<K, V> {
    private K key;
    private V value;
}