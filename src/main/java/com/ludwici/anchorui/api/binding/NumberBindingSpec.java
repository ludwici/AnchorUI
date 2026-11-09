package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record NumberBindingSpec(String source, double literal, double fallback) {
    public static NumberBindingSpec parse(JsonElement value, double defaultValue) {
        if (value == null || value.isJsonNull()) {
            return new NumberBindingSpec(null, defaultValue, defaultValue);
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            double number = value.getAsDouble();
            requireFinite(number);
            return new NumberBindingSpec(null, number, number);
        }
        if (!value.isJsonObject()) {
            throw new IllegalArgumentException("Numeric binding must be a number or an object with 'bind'");
        }
        JsonObject object = value.getAsJsonObject();
        JsonElement key = object.get("bind");
        if (key == null || !key.isJsonPrimitive() || !key.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Numeric binding requires a string 'bind' id");
        }
        String id = key.getAsString();
        if (id.isBlank()) {
            throw new IllegalArgumentException("Numeric binding id must not be empty");
        }
        JsonElement fallbackElement = object.get("fallback");
        double fallback = defaultValue;
        if (fallbackElement != null) {
            if (!fallbackElement.isJsonPrimitive() || !fallbackElement.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("Numeric binding fallback must be a number");
            }
            fallback = fallbackElement.getAsDouble();
            requireFinite(fallback);
        }
        return new NumberBindingSpec(id, defaultValue, fallback);
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Numeric binding values must be finite");
        }
    }
}
