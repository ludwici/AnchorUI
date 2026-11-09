package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class BoundData {
    private final DataBindingSpec spec;
    private final DataValueRegistry.Provider provider;
    private final List<String> path;

    private BoundData(DataBindingSpec spec, DataValueRegistry.Provider provider) {
        this.spec = spec;
        this.provider = provider;
        this.path = DataValuePaths.tokens(spec.path());
    }

    public static BoundData parse(JsonObject root, String field, JsonElement defaultValue, DataBindingSpec.Kind kind) {
        DataBindingSpec spec;
        try {
            spec = DataBindingSpec.parse(root.get(field), defaultValue, kind);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid '" + field + "': " + exception.getMessage(), exception);
        }
        if (!spec.isBound()) {
            return new BoundData(spec, null);
        }
        ResourceLocation id = ResourceLocation.tryParse(spec.source());
        if (id == null) {
            throw new IllegalArgumentException("Invalid '" + field + "' data source: " + spec.source());
        }
        DataValueRegistry.Provider provider = DataValueRegistry.find(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown '" + field + "' data source: " + id));
        return new BoundData(spec, provider);
    }

    public JsonElement get(WidgetContext context) {
        if (provider == null) {
            return spec.literal().deepCopy();
        }
        try {
            JsonElement raw = provider.read(context);
            JsonElement value = DataValuePaths.resolve(raw, path);
            if (spec.kind().matches(value)) {
                return value.deepCopy();
            }
        } catch (RuntimeException exception) {
            return spec.fallback().deepCopy();
        }
        return spec.fallback().deepCopy();
    }

    public boolean isBound() {
        return provider != null;
    }
}
