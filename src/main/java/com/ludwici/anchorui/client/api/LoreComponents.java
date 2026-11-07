package com.ludwici.anchorui.client.api;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.client.api.types.LoreModel;
import com.ludwici.anchorui.client.api.types.LoreText;
import com.ludwici.anchorui.client.api.types.StyledComponent;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public class LoreComponents {
    public String item = "";
    public List<StyledComponent> components = new ArrayList<>();

    public LoreComponents(JsonObject root) {
        var entities = GsonHelper.getAsJsonArray(root, "entities");
        entities.asList().forEach(jsonElement -> {
            JsonObject obj = jsonElement.getAsJsonObject();
            String type = GsonHelper.getAsString(obj, "type");
            switch (type) {
                case "text" -> components.add(new LoreText(obj));
                case "model" -> components.add(new LoreModel(obj));
                case "list" -> {}
                default -> System.err.println("Invalid entry type");
            }
        });
        item = GsonHelper.getAsString(root,  "item", "");
    }

    public void updateStyle() {
        components.forEach(StyledComponent::updateStyle);
    }

    public StyledComponent getComponent(String id) {
        return components.stream().filter(c -> c.id.equals(id)).findFirst().orElse(null);
    }
}
