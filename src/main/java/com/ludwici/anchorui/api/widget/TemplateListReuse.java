package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonObject;

import java.util.Objects;

public final class TemplateListReuse {
    private TemplateListReuse() {
    }

    public static boolean canReuse(JsonObject previous, JsonObject current,
                                   ItemContext previousItem, ItemContext currentItem) {
        return previous != null && previous.equals(current) && Objects.equals(previousItem, currentItem);
    }
}
