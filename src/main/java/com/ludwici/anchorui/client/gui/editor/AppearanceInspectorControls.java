package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.widget.WidgetEditorRegistry;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class AppearanceInspectorControls {
    public record Control(WidgetEditorRegistry.Field field, String value, boolean modified, String sourceValue) {
        public Control(WidgetEditorRegistry.Field field, String value, boolean modified) {
            this(field, value, modified, value);
        }

        public boolean textual() {
            return switch (field.kind()) {
                case TEXT, COLOR, TEXTURE -> true;
                default -> false;
            };
        }

        public boolean booleanField() {
            return field.kind() == WidgetEditorRegistry.Kind.BOOLEAN;
        }
    }

    private AppearanceInspectorControls() {
    }

    public static List<Control> describe(String id, ResourceLocation type, JsonObject settings,
                                         HudLayoutOffsets offsets) {
        List<Control> result = new ArrayList<>();
        for (WidgetEditorRegistry.Field field : WidgetEditorRegistry.fields(type)) {
            JsonElement source = read(settings, field.path());
            String value = source != null && (source.isJsonPrimitive() || isTextBinding(field, source))
                    ? displayValue(source) : field.fallback().getAsString();
            String original = value;
            JsonElement override = offsets.settingOverride(id, "appearance", field.path());
            if (override != null) {
                value = displayValue(override);
            }
            result.add(new Control(field, value, override != null && !sameValue(field, value, original), original));
        }
        return List.copyOf(result);
    }

    public static boolean sameValue(WidgetEditorRegistry.Field field, String current, String original) {
        try {
            return parseEditorValue(field, current).equals(parseEditorValue(field, original));
        } catch (RuntimeException exception) {
            return current.equals(original);
        }
    }

    public static JsonElement read(JsonObject settings, String path) {
        String[] segments = path.split("\\.");
        JsonElement current = settings;
        for (String segment : segments) {
            if (!current.isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject().get(segment);
            if (current == null || current.isJsonNull()) {
                return null;
            }
        }
        return current;
    }

    private static boolean isTextBinding(WidgetEditorRegistry.Field field, JsonElement value) {
        return field.kind() == WidgetEditorRegistry.Kind.TEXT && "text".equals(field.path()) && value.isJsonObject() && value.getAsJsonObject().has("bind");
    }

    public static String displayValue(JsonElement value) {
        return value.isJsonPrimitive() ? value.getAsString() : value.toString();
    }

    public static JsonElement parseEditorValue(WidgetEditorRegistry.Field field, String value) {
        JsonPrimitive literal = parse(field, value);
        if (field.kind() == WidgetEditorRegistry.Kind.TEXT && "text".equals(field.path()) && value.stripLeading().startsWith("{")) {
            JsonElement candidate;
            try {
                candidate = com.google.gson.JsonParser.parseString(value);
            } catch (com.google.gson.JsonParseException exception) {
                return literal;
            }
            if (isTextBinding(field, candidate)) {
                com.ludwici.anchorui.api.binding.DataBindingSpec.parse(candidate, new JsonPrimitive(""), com.ludwici.anchorui.api.binding.DataBindingSpec.Kind.STRING);
                return candidate;
            }
        }
        return literal;
    }

    public static JsonPrimitive parse(WidgetEditorRegistry.Field field, String value) {
        String trimmed = value.trim();
        return switch (field.kind()) {
            case TEXT -> {
                if (value.length() > field.max()) {
                    throw new IllegalArgumentException("Text exceeds " + (int) field.max() + " characters");
                }
                yield new JsonPrimitive(value);
            }
            case COLOR -> {
                if (!trimmed.matches("#(?:[a-fA-F0-9]{6}|[a-fA-F0-9]{8})")) {
                    throw new IllegalArgumentException("Color must be #RRGGBB or #AARRGGBB");
                }
                yield new JsonPrimitive(trimmed.toUpperCase(java.util.Locale.ROOT));
            }
            case TEXTURE -> {
                if (trimmed.isBlank() || ResourceLocation.tryParse(trimmed) == null) {
                    throw new IllegalArgumentException("Texture must be a valid namespaced resource location");
                }
                yield new JsonPrimitive(trimmed);
            }
            case INTEGER -> {
                int number = Integer.parseInt(trimmed);
                if (number < field.min() || number > field.max()) {
                    throw new IllegalArgumentException("Value must be between " + (int) field.min() + " and " + (int) field.max());
                }
                yield new JsonPrimitive(number);
            }
            case NUMBER -> {
                double number = Double.parseDouble(trimmed);
                if (!Double.isFinite(number) || number < field.min() || number > field.max()) {
                    throw new IllegalArgumentException("Value must be finite and within the configured range");
                }
                yield new JsonPrimitive(number);
            }
            case BOOLEAN -> {
                if (!"true".equalsIgnoreCase(trimmed) && !"false".equalsIgnoreCase(trimmed)) {
                    throw new IllegalArgumentException("Expected true or false");
                }
                yield new JsonPrimitive(Boolean.parseBoolean(trimmed));
            }
            case CHOICE -> {
                if (!field.choices().contains(trimmed)) {
                    throw new IllegalArgumentException("Expected one of " + field.choices());
                }
                yield new JsonPrimitive(trimmed);
            }
        };
    }

    public static JsonPrimitive stepped(Control control, int direction) {
        WidgetEditorRegistry.Field field = control.field();
        if (direction == 0) {
            throw new IllegalArgumentException("Step direction must not be zero");
        }
        if (field.kind() == WidgetEditorRegistry.Kind.BOOLEAN) {
            return new JsonPrimitive(!Boolean.parseBoolean(control.value()));
        }
        if (field.kind() == WidgetEditorRegistry.Kind.CHOICE) {
            List<String> values = field.choices();
            int index = Math.max(0, values.indexOf(control.value()));
            return new JsonPrimitive(values.get(Math.floorMod(index + (direction > 0 ? 1 : -1), values.size())));
        }
        if (field.kind() == WidgetEditorRegistry.Kind.INTEGER || field.kind() == WidgetEditorRegistry.Kind.NUMBER) {
            double number = Double.parseDouble(control.value());
            double next = Math.max(field.min(), Math.min(field.max(), number + (direction > 0 ? field.step() : -field.step())));
            return field.kind() == WidgetEditorRegistry.Kind.INTEGER
                    ? new JsonPrimitive((int) next) : new JsonPrimitive(next);
        }
        throw new IllegalArgumentException("Textual fields require the editor input");
    }
}
