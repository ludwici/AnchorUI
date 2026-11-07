package com.ludwici.anchorui.api;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LoreEntity {
    public String title = "";
    public String item = "";
    public String model = "";
    public float scale = 1.0F;
    public List<LoreComponent> components = new ArrayList<>();

    public LoreEntity() {

    }

    public LoreEntity(String title, String item, String model, float scale) {
        this.title = title;
        this.item = item;
        this.model = model;
        this.scale = scale;
        components = new ArrayList<>();
    }

    public LoreEntity(JsonObject root) {
        this(
                GsonHelper.getAsString(root, "title", ""),
                GsonHelper.getAsString(root, "item", ""),
                GsonHelper.getAsString(root, "model", ""),
                GsonHelper.getAsFloat(root, "scale", 1.0F)
        );
        var components = GsonHelper.getAsJsonArray(root, "lore");
        components.asList().forEach(jsonElement -> {
            this.components.add(new LoreComponent(jsonElement.getAsJsonObject()));
        });
    }

    public Map.Entry<String, String> getItemData() {
        var results = item.split(":");
        return Map.entry(results[0], results[1]);
    }

//    public LoreStyle getStyleSettings() {
//
//    }

}
