package com.ludwici.anchorui.api.widget;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class WidgetRegistryTable<K, V> {
    private final Map<K, V> factories = new ConcurrentHashMap<>();

    void register(K id, V factory) {
        Objects.requireNonNull(id, "Widget type id must not be null");
        Objects.requireNonNull(factory, "Widget factory must not be null");
        if (factories.putIfAbsent(id, factory) != null) {
            throw new IllegalStateException("Anchor UI widget type already registered: " + id);
        }
    }

    Optional<V> find(K id) {
        return Optional.ofNullable(factories.get(id));
    }

    boolean contains(K id) {
        return factories.containsKey(id);
    }
}
