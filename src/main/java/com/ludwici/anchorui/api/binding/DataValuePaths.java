package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class DataValuePaths {
    private DataValuePaths() {
    }

    public static List<String> tokens(String path) {
        if (path == null || path.isEmpty()) {
            return List.of();
        }
        if (path.length() > 256) {
            throw new IllegalArgumentException("Binding path is too long");
        }
        List<String> result = new ArrayList<>();
        StringBuilder name = new StringBuilder();
        for (int index = 0; index < path.length(); index++) {
            char ch = path.charAt(index);
            if (ch == '.') {
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Empty segment in binding path: " + path);
                }
                result.add(name.toString());
                name.setLength(0);
            } else if (ch == '[') {
                if (!name.isEmpty()) {
                    result.add(name.toString());
                    name.setLength(0);
                }
                int close = path.indexOf(']', index + 1);
                if (close < 0) {
                    throw new IllegalArgumentException("Missing closing bracket in binding path: " + path);
                }
                String number = path.substring(index + 1, close);
                if (!number.matches("[0-9]{1,6}")) {
                    throw new IllegalArgumentException("List index must be a non-negative integer: " + path);
                }
                result.add("[" + number + "]");
                index = close;
                if (index + 1 < path.length() && path.charAt(index + 1) != '.' && path.charAt(index + 1) != '[') {
                    throw new IllegalArgumentException("Unexpected character after list index: " + path);
                }
                if (index + 1 < path.length() && path.charAt(index + 1) == '.') {
                    index++;
                    if (index + 1 >= path.length()) {
                        throw new IllegalArgumentException("Trailing dot in binding path: " + path);
                    }
                }
            } else if (Character.isLetterOrDigit(ch) || ch == '_' || ch == '-') {
                name.append(ch);
            } else {
                throw new IllegalArgumentException("Invalid character in binding path: " + path);
            }
            if (result.size() > 32) {
                throw new IllegalArgumentException("Binding path exceeds 32 segments");
            }
        }
        if (!name.isEmpty()) {
            result.add(name.toString());
        } else if (path.endsWith(".")) {
            throw new IllegalArgumentException("Trailing dot in binding path: " + path);
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Empty binding path");
        }
        return List.copyOf(result);
    }

    public static void validate(String path) {
        tokens(path);
    }

    public static JsonElement resolve(JsonElement value, List<String> tokens) {
        JsonElement current = value;
        for (String token : tokens) {
            if (current == null || current.isJsonNull()) {
                return JsonNull.INSTANCE;
            }
            if (token.startsWith("[") && token.endsWith("]")) {
                if (!current.isJsonArray()) {
                    return JsonNull.INSTANCE;
                }
                int index = Integer.parseInt(token.substring(1, token.length() - 1));
                JsonArray array = current.getAsJsonArray();
                if (index >= array.size()) {
                    return JsonNull.INSTANCE;
                }
                current = array.get(index);
            } else {
                if (!current.isJsonObject()) {
                    return JsonNull.INSTANCE;
                }
                JsonObject object = current.getAsJsonObject();
                current = object.has(token) ? object.get(token) : JsonNull.INSTANCE;
            }
        }
        return current == null ? JsonNull.INSTANCE : current;
    }
}
