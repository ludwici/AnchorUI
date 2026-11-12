package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.ludwici.anchorui.api.binding.DataValuePaths;

import java.util.Objects;

/**
 * The data and stable key associated with a generated template-list row.
 * AnchorUI updates the index in place when a keyed row moves so widget factories,
 * bindings, conditions and actions can continue to read the current position.
 */
public final class ItemContext {
    private final JsonElement value;
    private final String key;
    private int index;

    public ItemContext(JsonElement value, String key, int index) {
        this.value = value == null ? JsonNull.INSTANCE : value.deepCopy();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Template list item key must not be blank");
        }
        this.key = key;
        moveToIndex(index);
    }

    public JsonElement value() {
        return value.deepCopy();
    }

    public String key() {
        return key;
    }

    public int index() {
        return index;
    }

    /**
     * Updates the index of an existing row without replacing the context held
     * by widgets and their data providers. Only AnchorUI should call this.
     */
    public void moveToIndex(int nextIndex) {
        if (nextIndex < 0) {
            throw new IllegalArgumentException("Template list item index must not be negative");
        }
        index = nextIndex;
    }

    public JsonElement get(String path) {
        JsonElement result = DataValuePaths.resolve(value, DataValuePaths.tokens(path));
        return result == null ? JsonNull.INSTANCE : result.deepCopy();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ItemContext that)) {
            return false;
        }
        return index == that.index && key.equals(that.key) && value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, key, index);
    }

    @Override
    public String toString() {
        return "ItemContext[value=" + value + ", key=" + key + ", index=" + index + "]";
    }
}
