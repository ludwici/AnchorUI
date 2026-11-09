package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public record DataBindingSpec(String source, String path, JsonElement literal, JsonElement fallback, Kind kind) {
    public enum Kind {
        NUMBER, STRING, BOOLEAN, OBJECT, LIST, ANY;

        public boolean matches(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return false;
            }
            return switch (this) {
                case NUMBER -> element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber() && Double.isFinite(element.getAsDouble());
                case STRING -> element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
                case BOOLEAN -> element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean();
                case OBJECT -> element.isJsonObject();
                case LIST -> element.isJsonArray();
                case ANY -> true;
            };
        }
    }

    public DataBindingSpec {
        if (kind == null || literal == null || fallback == null) {
            throw new IllegalArgumentException("Binding kind, literal and fallback must not be null");
        }
        literal = literal.deepCopy();
        fallback = fallback.deepCopy();
    }

    public static DataBindingSpec parse(JsonElement input, JsonElement defaultValue, Kind kind) {
        if (kind == null || defaultValue == null || !kind.matches(defaultValue)) {
            throw new IllegalArgumentException("Invalid default value for " + kind + " binding");
        }
        if (input == null || input.isJsonNull()) {
            return new DataBindingSpec(null, "", defaultValue, defaultValue, kind);
        }
        if (input.isJsonObject() && input.getAsJsonObject().has("bind")) {
            JsonObject config = input.getAsJsonObject();
            JsonElement key = config.get("bind");
            if (key == null || !key.isJsonPrimitive() || !key.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Binding 'bind' must be a namespaced string");
            }
            String source = key.getAsString();
            if (!source.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("Invalid namespaced binding id: " + source);
            }
            String path = "";
            if (config.has("path")) {
                JsonElement rawPath = config.get("path");
                if (!rawPath.isJsonPrimitive() || !rawPath.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Binding 'path' must be a string");
                }
                path = rawPath.getAsString();
                DataValuePaths.validate(path);
            }
            JsonElement fallback = config.has("fallback") ? config.get("fallback") : defaultValue;
            if (!kind.matches(fallback)) {
                throw new IllegalArgumentException("Invalid binding fallback; expected " + kind);
            }
            return new DataBindingSpec(source, path, defaultValue, fallback, kind);
        }
        if (!kind.matches(input)) {
            throw new IllegalArgumentException("Invalid literal binding; expected " + kind);
        }
        return new DataBindingSpec(null, "", input, input, kind);
    }

    public boolean isBound() {
        return source != null;
    }
}
