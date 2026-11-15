package com.ludwici.anchorui.api.state;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Transient state owned by one UI screen instance.
 *
 * <p>Only keys declared in the screen's initial state may be changed.
 * Values are copied on read/write so handlers cannot mutate the state
 * without passing validation.</p>
 */
public final class UiLocalState {
    private final Map<String, JsonElement> values = new LinkedHashMap<>();

    public UiLocalState(JsonObject initial) {
        Objects.requireNonNull(initial, "Initial UI state must not be null");
        for (var entry : initial.entrySet()) {
            checkKey(entry.getKey());
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull()) {
                throw new IllegalArgumentException("UI state '" + entry.getKey() + "' needs a non-null initial value");
            }
            values.put(entry.getKey(), value.deepCopy());
        }
    }

    public static UiLocalState rebased(JsonObject updatedDefaults, UiLocalState previous) {
        UiLocalState updated = new UiLocalState(updatedDefaults);
        JsonObject existing = previous.snapshot();
        for (var entry : updated.values.entrySet()) {
            JsonElement oldValue = existing.get(entry.getKey());
            if (oldValue != null && sameType(entry.getValue(), oldValue)) {
                updated.set(entry.getKey(), oldValue);
            }
        }
        return updated;
    }

    public synchronized JsonObject snapshot() {
        JsonObject object = new JsonObject();
        for (var entry : values.entrySet()) {
            object.add(entry.getKey(), entry.getValue().deepCopy());
        }
        return object;
    }

    public synchronized JsonElement get(String key) {
        checkDeclared(key);
        return values.get(key).deepCopy();
    }

    public synchronized void set(String key, JsonElement value) {
        checkDeclared(key);
        if (value == null || value.isJsonNull()) {
            throw new IllegalArgumentException("UI state value must not be null: " + key);
        }
        JsonElement current = values.get(key);
        if (!sameType(current, value)) {
            throw new IllegalArgumentException("UI state '" + key + "' cannot change value type");
        }
        values.put(key, value.deepCopy());
    }

    public synchronized boolean toggle(String key) {
        checkDeclared(key);
        JsonElement current = values.get(key);
        if (!current.isJsonPrimitive() || !current.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("UI state '" + key + "' must be boolean to toggle");
        }
        boolean next = !current.getAsBoolean();
        values.put(key, new JsonPrimitive(next));
        return next;
    }

    public synchronized void validateSet(String key, JsonElement value) {
        checkDeclared(key);
        if (value == null || value.isJsonNull() || !sameType(values.get(key), value)) {
            throw new IllegalArgumentException("Invalid value type for UI state '" + key + "'");
        }
    }

    public synchronized void validateToggle(String key) {
        checkDeclared(key);
        JsonElement current = values.get(key);
        if (!current.isJsonPrimitive() || !current.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("UI state '" + key + "' must be boolean to toggle");
        }
    }

    private static void checkKey(String key) {
        if (key == null || !key.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("UI state key must use letters, digits or underscores: " + key);
        }
    }

    private void checkDeclared(String key) {
        checkKey(key);
        if (!values.containsKey(key)) {
            throw new IllegalArgumentException("Unknown UI state key: " + key);
        }
    }

    private static boolean sameType(JsonElement left, JsonElement right) {
        if (left.isJsonObject()) {
            return right.isJsonObject();
        }
        if (left.isJsonArray()) {
            return right.isJsonArray();
        }
        if (!left.isJsonPrimitive() || !right.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive a = left.getAsJsonPrimitive();
        JsonPrimitive b = right.getAsJsonPrimitive();
        return (a.isString() && b.isString()) || (a.isNumber() && b.isNumber()) || (a.isBoolean() && b.isBoolean());
    }
}
