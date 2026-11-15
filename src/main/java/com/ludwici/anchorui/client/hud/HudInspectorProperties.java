package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.layout.WidgetContainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class HudInspectorProperties {
    private HudInspectorProperties() {
    }

    public static List<String> flatten(JsonObject settings) {
        List<String> lines = new ArrayList<>();
        if (settings != null) {
            String structuralParent = WidgetContainers.parent(settings);
            for (Map.Entry<String, JsonElement> field : settings.entrySet()) {
                if (field.getKey().startsWith("__")) {
                    continue;
                }
                if ("position".equals(field.getKey()) && structuralParent != null
                        && field.getValue().isJsonObject()) {
                    JsonObject position = field.getValue().getAsJsonObject().deepCopy();
                    if (position.has("relativeTo")
                            && WidgetContainers.contentId(structuralParent).equals(position.get("relativeTo").getAsString())) {
                        position.remove("relativeTo");
                    }
                    flatten(lines, "position", position);
                } else {
                    flatten(lines, field.getKey(), field.getValue());
                }
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
