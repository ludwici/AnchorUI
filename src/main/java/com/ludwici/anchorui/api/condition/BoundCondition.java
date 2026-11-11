package com.ludwici.anchorui.api.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.binding.DataValuePaths;
import com.ludwici.anchorui.api.binding.DataValueRegistry;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BoundCondition {
    private final ConditionExpression expression;
    private final Map<String, DataValueRegistry.Provider> providers;
    private final String property;
    private final Set<String> reportedFailures = new HashSet<>();

    private BoundCondition(ConditionExpression expression, Map<String, DataValueRegistry.Provider> providers, String property) {
        this.expression = expression;
        this.providers = providers;
        this.property = property;
    }

    public static BoundCondition parse(JsonObject json, String property, boolean defaultValue) {
        try {
            ConditionExpression expression = ConditionExpression.parse(json.get(property), defaultValue);
            Map<String, DataValueRegistry.Provider> providers = new HashMap<>();
            for (String source : expression.sources()) {
                ResourceLocation id = ResourceLocation.tryParse(source);
                if (id == null) {
                    throw new IllegalArgumentException("Invalid condition source: " + source);
                }
                providers.put(source, DataValueRegistry.find(id)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown condition data source: " + source)));
            }
            return new BoundCondition(expression, Map.copyOf(providers), property);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid '" + property + "': " + exception.getMessage(), exception);
        }
    }

    public boolean get(WidgetContext context) {
        try {
            return expression.evaluate((source, path) -> read(context, source, path));
        } catch (RuntimeException exception) {
            if (reportedFailures.add("condition")) {
                AnchorUI.LOGGER.warn("Anchor UI condition '{}' failed to evaluate; treating as false", property, exception);
            }
            return false;
        }
    }

    private JsonElement read(WidgetContext context, String source, List<String> path) {
        try {
            return DataValuePaths.resolve(providers.get(source).read(context), path);
        } catch (RuntimeException exception) {
            if (reportedFailures.add(source)) {
                AnchorUI.LOGGER.warn("Anchor UI condition '{}' data source '{}' failed; using fallback",
                        property, source, exception);
            }
            return null;
        }
    }
}
