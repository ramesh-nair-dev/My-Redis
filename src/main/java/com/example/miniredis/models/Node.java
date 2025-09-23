package com.example.miniredis.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Node<T>{
    public String key;
    public CacheValue<T> value;
    public Node<T> next;
    public Node<T> prev;

    public Node(String key, CacheValue<T> value) {
        this.key = key;
        this.value = value;
        this.next = null;
        this.prev = null;
    }
}
