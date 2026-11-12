package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Map;

public interface ContainerAutoLayout {
    record Arrangement(Map<String, StackLayout.Placement> placements, ContainerLayout.Size intrinsic) {
    }

    boolean collapseHidden();

    Arrangement arrange(int availableWidth, int availableHeight, List<StackLayout.Item> items,
                        boolean autoWidth, boolean autoHeight);

    static ContainerAutoLayout parse(JsonObject settings) {
        if (!settings.has("layout")) {
            return null;
        }
        JsonElement raw = settings.get("layout");
        String type;
        if (raw.isJsonObject()) {
            type = GsonHelper.getAsString(raw.getAsJsonObject(), "type", "column");
        } else if (raw.isJsonPrimitive() && raw.getAsJsonPrimitive().isString()) {
            type = raw.getAsString();
        } else {
            throw new IllegalArgumentException("Container layout must be row, column, grid, or an object");
        }
        return switch (type) {
            case "row", "column", "anchorui:row", "anchorui:column" -> StackLayout.Config.parse(settings);
            case "grid", "anchorui:grid" -> GridLayout.Config.parse(settings);
            default -> throw new IllegalArgumentException("Unknown container layout: " + type);
        };
    }
}
