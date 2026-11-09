package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.widget.WidgetContext;

public final class BoundBoolean {
    private final BoundData value;

    private BoundBoolean(BoundData value) {
        this.value = value;
    }

    public static BoundBoolean parse(JsonObject json, String property, boolean defaultValue) {
        return new BoundBoolean(BoundData.parse(json, property, new JsonPrimitive(defaultValue), DataBindingSpec.Kind.BOOLEAN));
    }

    public boolean get(WidgetContext context) {
        return value.get(context).getAsBoolean();
    }
}
