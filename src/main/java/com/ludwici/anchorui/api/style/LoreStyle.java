package com.ludwici.anchorui.api.style;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.Locale;

public class LoreStyle {
    private final JsonObject root;

    public LoreStyle(JsonObject root) {
        this.root = root.deepCopy();
    }

    public Element get(String type, JsonObject overrides) {
        JsonObject source = root.has(type) && root.get(type).isJsonObject() ? root.getAsJsonObject(type) : new JsonObject();
        JsonObject elementOverrides = overrides == null ? new JsonObject() : overrides.deepCopy();
        if (elementOverrides.has("text") && !elementOverrides.get("text").isJsonObject()) {
            elementOverrides.remove("text");
        }
        JsonObject merged = merge(source, elementOverrides);
        return new Element(merged);
    }

    private static JsonObject merge(JsonObject source, JsonObject overrides) {
        JsonObject result = source.deepCopy();
        for (var entry : overrides.entrySet()) {
            JsonElement original = result.get(entry.getKey());
            JsonElement value = entry.getValue();
            if (original != null && original.isJsonObject() && value.isJsonObject()) {
                result.add(entry.getKey(), merge(original.getAsJsonObject(), value.getAsJsonObject()));
            } else {
                result.add(entry.getKey(), value.deepCopy());
            }
        }
        return result;
    }

    public static class Element {
        public final PositionElement position;
        public final PaddingElement padding;
        public final SizeElement size;
        public final BackgroundElement background;
        public final int textColor;
        public final String textAlign;
        public final boolean textWrap;
        public final boolean hidden;

        public Element(JsonObject root) {
            position = new PositionElement(GsonHelper.getAsJsonObject(root, "position", new JsonObject()));
            padding = new PaddingElement(GsonHelper.getAsJsonObject(root, "padding", new JsonObject()));
            size = new SizeElement(GsonHelper.getAsJsonObject(root, "size", new JsonObject()));
            background = new BackgroundElement(GsonHelper.getAsJsonObject(root, "background", new JsonObject()));
            JsonObject text = root.has("text") && root.get("text").isJsonObject() ? root.getAsJsonObject("text") : new JsonObject();
            String hex = GsonHelper.getAsString(text, "color", "#FFE278").replace("#", "");
            if (root.has("color") && root.get("color").isJsonPrimitive()) {
                hex = root.get("color").getAsString().replace("#", "");
            }
            textColor = (int) Long.parseLong(hex, 16) | 0xFF000000;
            textAlign = GsonHelper.getAsString(root, "align", GsonHelper.getAsString(text, "align", "left")).toLowerCase(Locale.ROOT);
            textWrap = GsonHelper.getAsBoolean(root, "wrap", GsonHelper.getAsBoolean(text, "wrap", false));
            hidden = GsonHelper.getAsBoolean(root, "hidden", false);
        }
    }
}
