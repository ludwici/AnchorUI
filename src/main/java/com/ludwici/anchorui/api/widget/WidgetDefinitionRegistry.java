package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class WidgetDefinitionRegistry {
    private final Map<ResourceLocation, JsonObject> definitions;

    public WidgetDefinitionRegistry(Map<ResourceLocation, JsonObject> definitions) {
        Map<ResourceLocation, JsonObject> snapshot = new LinkedHashMap<>();
        for (var entry : definitions.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().deepCopy());
        }
        this.definitions = Map.copyOf(snapshot);
    }

    public Optional<JsonObject> find(ResourceLocation id) {
        JsonObject json = definitions.get(id);
        return json == null ? Optional.empty() : Optional.of(json.deepCopy());
    }

    public Set<ResourceLocation> ids() {
        return definitions.keySet();
    }

    public Map<String, JsonObject> exportForResolution() {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (var entry : definitions.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue().deepCopy());
        }
        return result;
    }
}
