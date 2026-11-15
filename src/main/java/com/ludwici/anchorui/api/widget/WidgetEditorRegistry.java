package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.WidgetTypeIds;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class WidgetEditorRegistry {
    public enum Kind {
        TEXT, COLOR, TEXTURE, INTEGER, NUMBER, BOOLEAN, CHOICE
    }

    public record Field(String path, String label, Kind kind, JsonPrimitive fallback,
                        double min, double max, double step, List<String> choices) {
        public Field {
            Objects.requireNonNull(path);
            Objects.requireNonNull(label);
            Objects.requireNonNull(kind);
            Objects.requireNonNull(fallback);
            choices = List.copyOf(choices);
            if (!path.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*){0,5}")
                    || path.equals("type") || path.equals("id")
                    || path.startsWith("on") || path.startsWith("position")
                    || path.startsWith("layout") || path.equals("stateKey")
                    || path.equals("ref") || path.equals("visibleWhen") || path.equals("enabledWhen")) {
                throw new IllegalArgumentException("Unsafe editable widget path: " + path);
            }
            if (min > max || !Double.isFinite(min) || !Double.isFinite(max)
                    || !Double.isFinite(step) || step <= 0) {
                throw new IllegalArgumentException("Invalid editor field range: " + path);
            }
            if (kind == Kind.CHOICE && choices.isEmpty()) {
                throw new IllegalArgumentException("Editor choice field requires options: " + path);
            }
        }

        public static Field text(String path, String label, String fallback) {
            return new Field(path, label, Kind.TEXT, new JsonPrimitive(fallback), 0, 4096, 1, List.of());
        }

        public static Field color(String path, String label, String fallback) {
            return new Field(path, label, Kind.COLOR, new JsonPrimitive(fallback), 0, 0, 1, List.of());
        }

        public static Field texture(String path, String label) {
            return new Field(path, label, Kind.TEXTURE, new JsonPrimitive(""), 0, 0, 1, List.of());
        }

        public static Field integer(String path, String label, int fallback, int min, int max, int step) {
            return new Field(path, label, Kind.INTEGER, new JsonPrimitive(fallback), min, max, step, List.of());
        }

        public static Field number(String path, String label, double fallback, double min, double max, double step) {
            return new Field(path, label, Kind.NUMBER, new JsonPrimitive(fallback), min, max, step, List.of());
        }

        public static Field bool(String path, String label, boolean fallback) {
            return new Field(path, label, Kind.BOOLEAN, new JsonPrimitive(fallback), 0, 1, 1, List.of());
        }

        public static Field choice(String path, String label, String fallback, String... choices) {
            return new Field(path, label, Kind.CHOICE, new JsonPrimitive(fallback), 0, 0, 1, List.of(choices));
        }
    }

    private static final Map<ResourceLocation, List<Field>> FIELDS = new ConcurrentHashMap<>();
    static {
        register(WidgetTypeIds.TEXT, List.of(
                Field.text("text", "Text", ""),
                Field.color("color", "Text color", "#FFE278"),
                Field.choice("align", "Text align", "left", "left", "center", "right"),
                Field.bool("wrap", "Text wrap", false),
                Field.integer("size.width", "Width", 0, 0, 16384, 10),
                Field.integer("size.height", "Height", 0, 0, 16384, 10)));
        register(WidgetTypeIds.BUTTON, List.of(
                Field.text("text", "Button text", ""),
                Field.color("color", "Text color", "#FFE278"),
                Field.color("fillColor", "Fill", "#FF28384B"),
                Field.color("borderColor", "Border", "#FF728CA3"),
                Field.color("hoverColor", "Hover", "#FF385E81"),
                Field.color("disabledColor", "Disabled", "#FF353B43"),
                Field.color("focusColor", "Focus outline", "#FFFFD16B"),
                Field.texture("textures.background.normal", "Button skin"),
                Field.texture("textures.background.hover", "Hover skin"),
                Field.texture("textures.background.focused", "Focused skin"),
                Field.texture("textures.background.hoverFocused", "Hover + focus skin"),
                Field.texture("textures.background.disabled", "Disabled skin"),
                Field.integer("size.width", "Width", 120, 1, 16384, 10),
                Field.integer("size.height", "Height", 20, 1, 16384, 2)));
        List<Field> panel = List.of(
                Field.color("fillColor", "Fill", "#00000000"),
                Field.color("borderColor", "Border", "#00000000"),
                Field.integer("borderWidth", "Border width", 0, 0, 256, 1),
                Field.integer("radius", "Radius", 0, 0, 256, 1),
                Field.texture("texture", "Texture"),
                Field.color("tint", "Texture tint", "#FFFFFFFF"),
                Field.integer("textureWidth", "Texture W", 256, 1, 16384, 10),
                Field.integer("textureHeight", "Texture H", 256, 1, 16384, 10),
                Field.integer("u", "Texture U", 0, 0, 16384, 1),
                Field.integer("v", "Texture V", 0, 0, 16384, 1),
                Field.integer("size.width", "Width", 120, 1, 16384, 10),
                Field.integer("size.height", "Height", 70, 1, 16384, 10));
        register(WidgetTypeIds.PANEL, panel);
        register(WidgetTypeIds.CONTAINER, panel);
        register(WidgetTypeIds.PROGRESS, List.of(
                Field.color("fillColor", "Fill", "#E85B60"),
                Field.color("background.color", "Track color", "#00000000"),
                Field.color("borderColor", "Frame color", "#00000000"),
                Field.integer("borderWidth", "Frame width", 0, 0, 256, 1),
                Field.texture("textures.background.normal", "Track texture"),
                Field.texture("textures.fill.normal", "Fill texture"),
                Field.texture("textures.frame.normal", "Frame texture"),
                Field.bool("showText", "Show values", false),
                Field.integer("size.width", "Width", 160, 1, 16384, 10),
                Field.integer("size.height", "Height", 14, 1, 16384, 2)));
        register(WidgetTypeIds.COLOR_PICKER, List.of(
                Field.text("text", "Label", "Color"),
                Field.color("color", "Text color", "#FFE278"),
                Field.color("fillColor", "Background", "#D923354A"),
                Field.color("borderColor", "Border", "#FF718699"),
                Field.integer("size.width", "Width", 288, 240, 16384, 10),
                Field.integer("size.height", "Height", 166, 152, 16384, 10)));
        register(WidgetTypeIds.IMAGE, List.of(
                Field.texture("texture", "Texture"),
                Field.color("tint", "Tint", "#FFFFFFFF"),
                Field.integer("textureWidth", "Texture W", 256, 1, 16384, 10),
                Field.integer("textureHeight", "Texture H", 256, 1, 16384, 10),
                Field.integer("u", "Texture U", 0, 0, 16384, 1),
                Field.integer("v", "Texture V", 0, 0, 16384, 1),
                Field.integer("size.width", "Width", 64, 1, 16384, 10),
                Field.integer("size.height", "Height", 64, 1, 16384, 10)));
        List<Field> stateFields = new ArrayList<>();
        stateFields.add(Field.text("text", "Label", ""));
        stateFields.add(Field.color("color", "Text color", "#FFE278"));
        stateFields.add(Field.color("fillColor", "Fill", "#D923354A"));
        stateFields.add(Field.color("borderColor", "Border", "#FF718699"));
        stateFields.add(Field.color("accentColor", "Accent", "#FF426D94"));
        stateFields.add(Field.color("hoverColor", "Hover", "#FF36506A"));
        stateFields.add(Field.color("disabledColor", "Disabled", "#FF353B43"));
        stateFields.add(Field.color("focusColor", "Focus outline", "#FFFFD16B"));
        stateFields.add(Field.texture("textures.background.normal", "Background skin"));
        stateFields.add(Field.texture("textures.background.hover", "Hover skin"));
        stateFields.add(Field.texture("textures.background.focused", "Focused skin"));
        stateFields.add(Field.texture("textures.background.hoverFocused", "Hover + focus skin"));
        stateFields.add(Field.texture("textures.background.disabled", "Disabled skin"));
        for (String part : List.of("indicator", "track", "thumb")) {
            stateFields.add(Field.texture("textures." + part + ".off", part + " off"));
            stateFields.add(Field.texture("textures." + part + ".on", part + " on"));
            stateFields.add(Field.texture("textures." + part + ".offHover", part + " off hover"));
            stateFields.add(Field.texture("textures." + part + ".onHover", part + " on hover"));
        }
        stateFields.add(Field.texture("textures.arrow.closed", "Arrow closed"));
        stateFields.add(Field.texture("textures.arrow.open", "Arrow open"));
        stateFields.add(Field.texture("textures.popup.normal", "Popup skin"));
        stateFields.add(Field.texture("textures.option.normal", "Option skin"));
        stateFields.add(Field.texture("textures.option.selected", "Selected option"));
        stateFields.add(Field.texture("textures.tab.normal", "Tab skin"));
        stateFields.add(Field.texture("textures.tab.selected", "Selected tab"));
        stateFields.add(Field.integer("size.width", "Width", 150, 1, 16384, 10));
        stateFields.add(Field.integer("size.height", "Height", 22, 1, 16384, 2));
        for (ResourceLocation id : List.of(WidgetTypeIds.CHECKBOX, WidgetTypeIds.SWITCH,
                WidgetTypeIds.SELECT, WidgetTypeIds.TABS)) {
            register(id, stateFields);
        }
    }

    private WidgetEditorRegistry() {
    }

    public static void register(ResourceLocation type, List<Field> fields) {
        Objects.requireNonNull(type);
        List<Field> values = List.copyOf(fields);
        java.util.Set<String> names = new java.util.HashSet<>();
        for (Field field : values) {
            if (!names.add(field.path())) {
                throw new IllegalArgumentException("Duplicate editor field: " + field.path());
            }
        }
        if (FIELDS.putIfAbsent(type, values) != null) {
            throw new IllegalArgumentException("Editor fields already registered for " + type);
        }
    }

    public static List<Field> fields(ResourceLocation type) {
        return FIELDS.getOrDefault(type, List.of());
    }

    public static Field find(ResourceLocation type, String path) {
        for (Field field : fields(type)) {
            if (field.path().equals(path)) {
                return field;
            }
        }
        return null;
    }
}
