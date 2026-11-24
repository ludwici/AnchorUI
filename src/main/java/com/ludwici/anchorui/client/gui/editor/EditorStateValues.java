package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EditorStateValues {
    private EditorStateValues() {
    }

    public static List<String> choices(String key, JsonElement current, JsonElement initial, Iterable<JsonObject> definitions) {
        Set<String> values = new LinkedHashSet<>();
        values.add(current.toString());
        add(values, current, initial);
        if (current.isJsonPrimitive() && current.getAsJsonPrimitive().isBoolean()) {
            values.add("true");
            values.add("false");
        }
        for (JsonObject definition : definitions) {
            collect(definition, key, current, values);
        }
        return List.copyOf(values);
    }

    private static void collect(JsonElement node, String key, JsonElement current, Set<String> values) {
        if (node.isJsonArray()) {
            for (JsonElement child : node.getAsJsonArray()) {
                collect(child, key, current, values);
            }
        } else if (node.isJsonObject()) {
            JsonObject object = node.getAsJsonObject();
            if (new JsonPrimitive("anchorui:set_state").equals(object.get("type")) && new JsonPrimitive(key).equals(object.get("key"))) {
                add(values, current, object.get("value"));
            }
            for (var entry : object.entrySet()) {
                collect(entry.getValue(), key, current, values);
            }
        }
    }

    private static void add(Set<String> values, JsonElement current, JsonElement candidate) {
        if (candidate == null || candidate.isJsonNull()) {
            return;
        }
        boolean matches = current.isJsonArray() && candidate.isJsonArray() || current.isJsonObject() && candidate.isJsonObject();
        if (current.isJsonPrimitive() && candidate.isJsonPrimitive()) {
            JsonPrimitive a = current.getAsJsonPrimitive();
            JsonPrimitive b = candidate.getAsJsonPrimitive();
            matches = a.isString() && b.isString() || a.isBoolean() && b.isBoolean() || a.isNumber() && b.isNumber();
        }
        if (matches) {
            values.add(candidate.toString());
        }
    }

    public static String inputValue(JsonElement value) {
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : value.toString();
    }

    public static JsonElement parseInput(JsonElement current, String input) {
        return current.isJsonPrimitive() && current.getAsJsonPrimitive().isString() ? new JsonPrimitive(input) : JsonParser.parseString(input);
    }
}
