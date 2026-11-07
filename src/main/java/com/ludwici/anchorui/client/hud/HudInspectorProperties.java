package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class HudInspectorProperties {
    private HudInspectorProperties() {
    }

    public static List<String> flatten(JsonObject settings) {
        List<String> lines = new ArrayList<>();
        if (settings != null) {
            for (Map.Entry<String, JsonElement> field : settings.entrySet()) {
                flatten(lines, field.getKey(), field.getValue());
            }
        }
        return List.copyOf(lines);
    }

    private static void flatten(List<String> lines, String key, JsonElement value) {
        if (value == null || value.isJsonNull()) {
            lines.add(key + ": null");
        } else if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.isEmpty()) {
                lines.add(key + ": {}");
            } else {
                for (Map.Entry<String, JsonElement> child : object.entrySet()) {
                    flatten(lines, key + "." + child.getKey(), child.getValue());
                }
            }
        } else if (value.isJsonArray()) {
            if (value.getAsJsonArray().isEmpty()) {
                lines.add(key + ": []");
            } else {
                for (int i = 0; i < value.getAsJsonArray().size(); i++) {
                    flatten(lines, key + "[" + i + "]", value.getAsJsonArray().get(i));
                }
            }
        } else {
            lines.add(key + ": " + (value.getAsJsonPrimitive().isString()
                    ? value.getAsString().replace("\n", "\\n") : value.toString()));
        }
    }
}
