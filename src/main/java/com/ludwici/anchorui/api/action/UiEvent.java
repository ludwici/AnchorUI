package com.ludwici.anchorui.api.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import java.util.Objects;

public record UiEvent(String name, String targetId, JsonElement value) {
    public UiEvent {
        Objects.requireNonNull(name, "UI event name must not be null");
        Objects.requireNonNull(targetId, "UI event target must not be null");
        value = value == null ? JsonNull.INSTANCE : value.deepCopy();
    }

    @Override
    public JsonElement value() {
        return value.deepCopy();
    }

    public static UiEvent of(String name, String targetId) {
        return new UiEvent(name, targetId, JsonNull.INSTANCE);
    }
}
