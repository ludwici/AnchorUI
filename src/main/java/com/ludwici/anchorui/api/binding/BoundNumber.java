package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;

public final class BoundNumber {
    private final NumericDataRegistry.Provider provider;
    private final double literal;
    private final double fallback;

    private BoundNumber(NumericDataRegistry.Provider provider, double literal, double fallback) {
        this.provider = provider;
        this.literal = literal;
        this.fallback = fallback;
    }

    public static BoundNumber parse(JsonObject root, String field, double defaultValue) {
        NumberBindingSpec spec;
        try {
            spec = NumberBindingSpec.parse(root.get(field), defaultValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid '" + field + "': " + exception.getMessage(), exception);
        }
        if (spec.source() == null) {
            return new BoundNumber(null, spec.literal(), spec.fallback());
        }
        ResourceLocation id = ResourceLocation.tryParse(spec.source());
        if (id == null || !spec.source().contains(":")) {
            throw new IllegalArgumentException("Invalid '" + field + "' data source: " + spec.source() + "; use namespace:path");
        }
        NumericDataRegistry.Provider provider = NumericDataRegistry.find(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown '" + field + "' data source: " + id));
        return new BoundNumber(provider, spec.literal(), spec.fallback());
    }

    public double get(WidgetContext context) {
        if (provider == null) {
            return literal;
        }
        try {
            double result = provider.read(context);
            return Double.isFinite(result) ? result : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
