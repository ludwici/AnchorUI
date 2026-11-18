package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.WidgetTypeIds;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Creatable widget presets. Other mods may register a default for their own widget type. */
public final class EditorWidgetCreation {
    private static final Map<ResourceLocation, Supplier<JsonObject>> PRESETS = new LinkedHashMap<>();

    static {
        register(WidgetTypeIds.TEXT, () -> {
            JsonObject json = base(120, 18);
            json.addProperty("text", "New text");
            json.addProperty("color", "#FFFFFFFF");
            return json;
        });
        register(WidgetTypeIds.PANEL, () -> {
            JsonObject json = base(150, 90);
            json.addProperty("fillColor", "#99344D63");
            json.addProperty("borderColor", "#FF80BCCB");
            json.addProperty("borderWidth", 1);
            return json;
        });
        register(WidgetTypeIds.CONTAINER, () -> {
            JsonObject json = base(150, 90);
            json.addProperty("fillColor", "#99344D63");
            return json;
        });
        register(WidgetTypeIds.BUTTON, () -> {
            JsonObject json = base(120, 20);
            json.addProperty("text", "New button");
            return json;
        });
        register(WidgetTypeIds.PROGRESS, () -> {
            JsonObject json = base(160, 14);
            json.addProperty("value", 75);
            json.addProperty("max", 100);
            return json;
        });
    }

    private EditorWidgetCreation() {
    }

    public static void register(ResourceLocation type, Supplier<JsonObject> defaults) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(defaults);
        if (PRESETS.putIfAbsent(type, defaults) != null) {
            throw new IllegalStateException("Widget creation preset already registered: " + type);
        }
    }

    public static List<String> types() {
        return PRESETS.keySet().stream().map(ResourceLocation::toString).sorted().toList();
    }

    public static JsonObject newWidget(String type, String id, int offsetX, int offsetY) {
        ResourceLocation widgetType = WidgetTypeIds.parse(type);
        Supplier<JsonObject> source = PRESETS.get(widgetType);
        if (source == null) {
            throw new IllegalArgumentException("No editor creation preset for " + widgetType);
        }
        JsonObject json = Objects.requireNonNull(source.get(), "Widget preset cannot be null").deepCopy();
        json.addProperty("type", widgetType.toString());
        json.addProperty("id", id);
        JsonObject position = new JsonObject();
        position.addProperty("point", "CENTER");
        position.addProperty("anchor", "CENTER");
        position.addProperty("relativeTo", "screen");
        position.addProperty("x", offsetX);
        position.addProperty("y", offsetY);
        json.add("position", position);
        return json;
    }

    private static JsonObject base(int width, int height) {
        JsonObject json = new JsonObject();
        JsonObject size = new JsonObject();
        size.addProperty("width", width);
        size.addProperty("height", height);
        json.add("size", size);
        return json;
    }
}
