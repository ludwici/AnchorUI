package com.ludwici.anchorui.api.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import java.util.Objects;

public record UiEvent(String name, String targetId, JsonElement value, JsonElement item) {
    public UiEvent {
        Objects.requireNonNull(name, "UI event name must not be null");
        Objects.requireNonNull(targetId, "UI event target must not be null");
        value = value == null ? JsonNull.INSTANCE : value.deepCopy();
        item = item == null ? JsonNull.INSTANCE : item.deepCopy();
    }

    public UiEvent(String name, String targetId, JsonElement value) {
        this(name, targetId, value, JsonNull.INSTANCE);
    }

    @Override
    public JsonElement value() {
        return value.deepCopy();
    }

    @Override
    public JsonElement item() {
        return item.deepCopy();
    }

    public static UiEvent of(String name, String targetId) {
        return new UiEvent(name, targetId, JsonNull.INSTANCE);
    }
}
