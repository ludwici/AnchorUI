package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonObject;

import java.util.Objects;

public final class TemplateListReuse {
    private TemplateListReuse() {
    }

    /**
     * Keyed rows may move without recreating their widgets. The expander
     * generates position.x/y from the current list index, so those two
     * coordinates are ignored only when the same key and item are retained.
     * Other changes to a widget definition still require a fresh instance.
     */
    public static boolean canReuse(JsonObject previous, JsonObject current,
                                   ItemContext previousItem, ItemContext currentItem) {
        if (previous == null || current == null) {
            return false;
        }
        if (previousItem == null || currentItem == null) {
            return previous.equals(current) && Objects.equals(previousItem, currentItem);
        }
        if (!previousItem.key().equals(currentItem.key())
                || !previousItem.value().equals(currentItem.value())) {
            return false;
        }
        JsonObject oldSettings = previous.deepCopy();
        JsonObject newSettings = current.deepCopy();
        withoutGeneratedCoordinates(oldSettings);
        withoutGeneratedCoordinates(newSettings);
        return oldSettings.equals(newSettings);
    }

    private static void withoutGeneratedCoordinates(JsonObject settings) {
        if (!settings.has("position") || !settings.get("position").isJsonObject()) {
            return;
        }
        JsonObject position = settings.getAsJsonObject("position");
        position.remove("x");
        position.remove("y");
    }
}
