package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class BoundData {
    private final DataBindingSpec spec;
    private final DataSourceSnapshot source;
    private final List<String> path;
    private final DataFormatterRegistry.Formatter formatter;

    private BoundData(DataBindingSpec spec, DataValueRegistry.Provider provider, DataFormatterRegistry.Formatter formatter) {
        this.spec = spec;
        this.source = provider == null ? null : new DataSourceSnapshot(provider);
        this.formatter = formatter;
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
            return new BoundData(spec, null, null);
        }
        ResourceLocation id = ResourceLocation.tryParse(spec.source());
        if (id == null) {
            throw new IllegalArgumentException("Invalid '" + field + "' data source: " + spec.source());
        }
        DataValueRegistry.Provider provider = DataValueRegistry.find(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown '" + field + "' data source: " + id));
        JsonElement config = root.get(field);
        DataFormatterRegistry.Formatter formatter = DataFormatterRegistry.compile(config != null && config.isJsonObject() ? config.getAsJsonObject().get("format") : null);
        return new BoundData(spec, provider, formatter);
    }

    public JsonElement get(WidgetContext context) {
        if (source == null) {
            return spec.literal().deepCopy();
        }
        try {
            JsonElement raw = source.read(context);
            JsonElement value = DataValuePaths.resolve(raw, path);
            if (value == null || value.isJsonNull()) {
                return spec.fallback().deepCopy();
            }
            if (formatter != null) {
                value = formatter.format(value.deepCopy(), context);
            }
            if (spec.kind().matches(value)) {
                return value.deepCopy();
            }
        } catch (RuntimeException exception) {
            return spec.fallback().deepCopy();
        }
        return spec.fallback().deepCopy();
    }

    public boolean isBound() {
        return source != null;
    }
}
