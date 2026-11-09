package com.ludwici.anchorui.api.binding;

import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class NumericDataRegistry {
    @FunctionalInterface
    public interface Provider {
        double read(WidgetContext context);
    }

    private static final java.util.Map<ResourceLocation, Provider> PROVIDERS = new java.util.concurrent.ConcurrentHashMap<>();

    private NumericDataRegistry() {
    }

    public static void register(ResourceLocation id, Provider provider) {
        java.util.Objects.requireNonNull(id, "Numeric data id must not be null");
        java.util.Objects.requireNonNull(provider, "Numeric data provider must not be null");
        if (PROVIDERS.putIfAbsent(id, provider) != null) {
            throw new IllegalStateException("Anchor UI numeric data provider already registered: " + id);
        }
        try {
            DataValueRegistry.registerNumber(id, provider::read);
        } catch (RuntimeException exception) {
            PROVIDERS.remove(id, provider);
            throw exception;
        }
    }

    public static Optional<Provider> find(ResourceLocation id) {
        return Optional.ofNullable(PROVIDERS.get(id));
    }
}
