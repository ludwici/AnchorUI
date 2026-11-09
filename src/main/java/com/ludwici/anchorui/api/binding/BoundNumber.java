package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.widget.WidgetContext;

public final class BoundNumber {
    private final BoundData data;
    private final double defaultValue;

    private BoundNumber(BoundData data, double defaultValue) {
        this.data = data;
        this.defaultValue = defaultValue;
    }

    public static BoundNumber parse(JsonObject root, String field, double defaultValue) {
        if (!Double.isFinite(defaultValue)) {
            throw new IllegalArgumentException("Default numeric value must be finite");
        }
        return new BoundNumber(BoundData.parse(root, field, new JsonPrimitive(defaultValue), DataBindingSpec.Kind.NUMBER), defaultValue);
    }

    public double get(WidgetContext context) {
        double result = data.get(context).getAsDouble();
        return Double.isFinite(result) ? result : defaultValue;
    }
}
