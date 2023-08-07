package org.example.repository;

public record EntityKey<T>(Class<T> type,Object id) {
}
