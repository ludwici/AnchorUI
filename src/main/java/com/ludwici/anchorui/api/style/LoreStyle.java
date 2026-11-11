package com.ludwici.anchorui.api.style;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;

import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.layout.ContainerLayout;
import com.ludwici.anchorui.api.layout.ContainerScroll;

import java.util.Locale;

public class LoreStyle {
    private final JsonObject root;

    public LoreStyle(JsonObject root) {
        this.root = root.deepCopy();
    }

    public Element get(ResourceLocation type, JsonObject overrides) {
        JsonObject source = new JsonObject();
        if (root.has(type.toString()) && root.get(type.toString()).isJsonObject()) {
            source = root.getAsJsonObject(type.toString());
        } else if (type.getNamespace().equals(AnchorUI.MODID) && root.has(type.getPath()) && root.get(type.getPath()).isJsonObject()) {
            source = root.getAsJsonObject(type.getPath());
        }
        return new Element(merge(source, sanitizeOverrides(overrides)));
    }

    public Element get(String type, JsonObject overrides) {
        JsonObject source = root.has(type) && root.get(type).isJsonObject() ? root.getAsJsonObject(type) : new JsonObject();
        return new Element(merge(source, sanitizeOverrides(overrides)));
    }

    private static JsonObject sanitizeOverrides(JsonObject overrides) {
        JsonObject result = overrides == null ? new JsonObject() : overrides.deepCopy();
        if (result.has("text") && !result.get("text").isJsonObject()) {
            result.remove("text");
        }
        return result;
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
        public final ContainerLayout.AutoSize autoSize;
        public final ContainerScroll.Config scroll;

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
            autoSize = ContainerLayout.AutoSize.parse(root);
            scroll = ContainerScroll.Config.parse(root);
        }
    }
}
