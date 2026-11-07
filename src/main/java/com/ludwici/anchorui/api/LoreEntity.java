package com.ludwici.anchorui.api;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public class LoreEntity {
    public final String title;
    public final String item;
    public final String model;
    public final String style;
    public final float scale;
    public final String namespace;
    public final boolean hasExplicitWidgets;
    public final List<LoreComponent> components = new ArrayList<>();
    public final List<UiElement> entities = new ArrayList<>();

    public LoreEntity(JsonObject root) {
        this(root, "anchorui");
    }

    public LoreEntity(JsonObject root, String namespace) {
        this.namespace = namespace;
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
            text = GsonHelper.getAsString(root, "text", "");
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
