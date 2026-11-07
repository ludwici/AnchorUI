package com.ludwici.anchorui.api.widget;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class AnchorWidgetRegistry {
    private static final Map<ResourceLocation, WidgetFactory> FACTORIES = new ConcurrentHashMap<>();

    private AnchorWidgetRegistry() {
    }

    public static void register(ResourceLocation id, WidgetFactory factory) {
        Objects.requireNonNull(id, "Widget type id must not be null");
        Objects.requireNonNull(factory, "Widget factory must not be null");
        if (FACTORIES.putIfAbsent(id, factory) != null) {
            throw new IllegalStateException("Anchor UI widget type already registered: " + id);
        }
    }

    public static Optional<WidgetFactory> find(ResourceLocation id) {
        return Optional.ofNullable(FACTORIES.get(id));
    }

    public static boolean contains(ResourceLocation id) {
        return FACTORIES.containsKey(id);
    }
}
