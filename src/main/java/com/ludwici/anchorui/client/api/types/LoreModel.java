package com.ludwici.anchorui.client.api.types;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public class LoreModel extends StyledComponent {
    public float scale = 4.0F;
    public String model = "";

    public LoreModel(JsonObject root, String model, float scale) {
        super(root);
        this.model = model;
        this.scale = scale;
    }

    public LoreModel(JsonObject root) {
        this(
                root,
                GsonHelper.getAsString(root,"path", ""),
                GsonHelper.getAsFloat(root,"scale", 1.0F)
        );
    }

    @Override
    public String getType() {
        return "model";
    }

}
