package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.ludwici.anchorui.api.binding.DataValuePaths;

/**
 * The immutable data record associated with a generated template-list row.
 * It is available in WidgetContext and used for UI event metadata.
 */
public record ItemContext(JsonElement value, String key, int index) {
    public ItemContext {
        value = value == null ? JsonNull.INSTANCE : value.deepCopy();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Template list item key must not be blank");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Template list item index must not be negative");
        }
    }

    @Override
    public JsonElement value() {
        return value.deepCopy();
    }

    public JsonElement get(String path) {
        JsonElement result = DataValuePaths.resolve(value, DataValuePaths.tokens(path));
        return result == null ? JsonNull.INSTANCE : result.deepCopy();
    }
}
