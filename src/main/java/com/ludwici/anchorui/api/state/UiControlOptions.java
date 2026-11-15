package com.ludwici.anchorui.api.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates the choices offered by an AnchorUI select or tabs widget.
 * Choices may be strings/numbers or objects with value and optional label.
 */
public final class UiControlOptions {
    public record Choice(JsonElement value, String label, JsonElement style) {
        public Choice(JsonElement value, String label) {
            this(value, label, null);
        }

        public Choice {
            if (value == null || !value.isJsonPrimitive()
                    || value.getAsJsonPrimitive().isBoolean()) {
                throw new IllegalArgumentException("Choice value must be a string or number");
            }
            value = value.deepCopy();
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("Choice label must not be empty");
            }
            if (style != null) {
                if (style.isJsonObject()) {
                    style = style.deepCopy();
                } else if (style.isJsonPrimitive() && style.getAsJsonPrimitive().isString()
                        && !style.getAsString().isBlank()) {
                    style = style.deepCopy();
                } else {
                    throw new IllegalArgumentException("Choice style must be a non-empty style id or an object");
                }
            }
        }

        @Override
        public JsonElement value() {
            return value.deepCopy();
        }

        @Override
        public JsonElement style() {
            return style == null ? null : style.deepCopy();
        }
    }

    private UiControlOptions() {
    }

    public static List<Choice> parse(JsonObject settings, UiLocalState state, String key) {
        if (!settings.has("options") || !settings.get("options").isJsonArray()) {
            throw new IllegalArgumentException("UI select/tabs requires an 'options' array");
        }
        JsonArray options = settings.getAsJsonArray("options");
        if (options.isEmpty() || options.size() > 64) {
            throw new IllegalArgumentException("UI options count must be between 1 and 64");
        }

        Set<JsonElement> ids = new HashSet<>();
        List<Choice> result = new ArrayList<>();
        for (JsonElement item : options) {
            JsonElement value;
            String label;
            JsonElement choiceStyle = null;
            if (item.isJsonPrimitive()) {
                value = item;
                label = item.getAsString();
            } else if (item.isJsonObject()) {
                JsonObject object = item.getAsJsonObject();
                value = object.get("value");
                choiceStyle = object.get("style");
                label = GsonHelper.getAsString(object, "label",
                        value != null && value.isJsonPrimitive() ? value.getAsString() : "");
            } else {
                throw new IllegalArgumentException("Choice must be a string/number or an object with 'value'");
            }
            Choice choice = new Choice(value, label, choiceStyle);
            state.validateSet(key, choice.value());
            if (!ids.add(choice.value())) {
                throw new IllegalArgumentException("Duplicate UI choice value: " + value);
            }
            result.add(choice);
        }

        JsonElement current = state.get(key);
        if (!ids.contains(current)) {
            throw new IllegalArgumentException("Initial UI state '" + key + "' is not present in options");
        }
        return List.copyOf(result);
    }

    public static int selectedIndex(List<Choice> choices, JsonElement value) {
        for (int index = 0; index < choices.size(); index++) {
            if (choices.get(index).value().equals(value)) {
                return index;
            }
        }
        return -1;
    }
}
