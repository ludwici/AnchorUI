package com.ludwici.anchorui.api.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UiStateHandoff {
    private UiStateHandoff() {
    }

    public static Map<String, String> parse(JsonObject settings, UiLocalState source) {
        if (!settings.has("stateHandoff")) {
            return Map.of();
        }
        JsonElement raw = settings.get("stateHandoff");
        if (!raw.isJsonObject()) {
            throw new IllegalArgumentException("open_screen stateHandoff must be an object mapping destination keys to source keys");
        }
        Map<String, String> mapping = new LinkedHashMap<>();
        for (var entry : raw.getAsJsonObject().entrySet()) {
            JsonElement key = entry.getValue();
            if (!key.isJsonPrimitive() || !key.getAsJsonPrimitive().isString() || key.getAsString().isBlank()) {
                throw new IllegalArgumentException("stateHandoff." + entry.getKey() + " requires a source key string");
            }
            String sourceKey = key.getAsString();
            try {
                source.get(sourceKey);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("stateHandoff." + entry.getKey() + " references unknown source state '" + sourceKey + "'", exception);
            }
            mapping.put(entry.getKey(), sourceKey);
        }
        return Map.copyOf(mapping);
    }

    public static void copy(UiLocalState source, UiLocalState destination, Map<String, String> mapping) {
        Map<String, JsonElement> pending = new LinkedHashMap<>();
        JsonObject sourceValues = source.snapshot();
        for (var entry : mapping.entrySet()) {
            String targetKey = entry.getKey();
            String sourceKey = entry.getValue();
            JsonElement value = sourceValues.get(sourceKey);
            if (value == null) {
                throw new IllegalArgumentException("Missing source state '" + sourceKey + "' for destination key '" + targetKey + "'");
            }
            try {
                destination.validateSet(targetKey, value);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Cannot transfer UI state '" + sourceKey + "' to '" + targetKey + "': " + exception.getMessage(), exception);
            }
            pending.put(targetKey, value.deepCopy());
        }
        for (var entry : pending.entrySet()) {
            destination.set(entry.getKey(), entry.getValue());
        }
    }
}
