package com.example.miniredis.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CacheResponse<T> {
    private String key;
    private T value;
}