package com.ludwici.anchorui.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class LoreEntity {
    public final String title;
    public final String item;
    public final String model;
    public final String style;
    public final float scale;
    public final String namespace;
    public final JsonElement onOpen;
    public final JsonElement onClose;
    public final JsonObject initialState;
    public final JsonObject animation;
    public final boolean hasExplicitWidgets;
    public final List<LoreComponent> components = new ArrayList<>();
    public final List<UiElement> entities = new ArrayList<>();
    public final Map<String, String> widgetParents = new LinkedHashMap<>();

    public LoreEntity(JsonObject root) {
        this(root, "anchorui");
    }

    public LoreEntity(JsonObject root, String namespace) {
        this.namespace = namespace;
        if (root.has("state") && !root.get("state").isJsonObject()) {
            throw new IllegalArgumentException("Screen 'state' must be a JSON object");
        }
        initialState = root.has("state") ? root.getAsJsonObject("state").deepCopy() : new JsonObject();
        if (root.has("animation") && !root.get("animation").isJsonObject()) {
            throw new IllegalArgumentException("Screen 'animation' must be an object");
        }
        animation = root.has("animation") ? root.getAsJsonObject("animation").deepCopy() : new JsonObject();
        onOpen = root.has("onOpen") ? root.get("onOpen").deepCopy() : null;
        onClose = root.has("onClose") ? root.get("onClose").deepCopy() : null;
        this.hasExplicitWidgets = root.has("entities");
        title = GsonHelper.getAsString(root, "title", "");
        item = GsonHelper.getAsString(root, "item", "");
        model = GsonHelper.getAsString(root, "model", "");
        style = GsonHelper.getAsString(root, "style", "anchorui:style/default");
        scale = GsonHelper.getAsFloat(root, "scale", 1.0F);
        if (root.has("lore")) {
            for (var entry : GsonHelper.getAsJsonArray(root, "lore")) {
                components.add(new LoreComponent(entry.getAsJsonObject()));
            }
        }
        if (root.has("widgetParents")) {
            JsonObject parentDefinitions = GsonHelper.getAsJsonObject(root, "widgetParents");
            for (var entry : parentDefinitions.entrySet()) {
                widgetParents.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        if (root.has("entities")) {
            for (var entry : GsonHelper.getAsJsonArray(root, "entities")) {
                entities.add(new UiElement(entry.getAsJsonObject()));
            }
        }
    }

    public static class UiElement {
        public final ResourceLocation type;
        public final String id;
        public final String text;
        public final String path;
        public final float scale;
        public final JsonObject settings;
        public final List<LoreComponent> lore = new ArrayList<>();

        public UiElement(JsonObject root) {
            type = WidgetTypeIds.parse(GsonHelper.getAsString(root, "type"));
            id = GsonHelper.getAsString(root, "id", "");
            text = root.has("text") && root.get("text").isJsonPrimitive() && root.get("text").getAsJsonPrimitive().isString() ? root.get("text").getAsString() : "";
            path = GsonHelper.getAsString(root, "path", "");
            scale = GsonHelper.getAsFloat(root, "scale", 1.0F);
            settings = root.deepCopy();
            if (root.has("lore")) {
                for (var entry : GsonHelper.getAsJsonArray(root, "lore")) {
                    lore.add(new LoreComponent(entry.getAsJsonObject()));
                }
            }
        }
    }
}
