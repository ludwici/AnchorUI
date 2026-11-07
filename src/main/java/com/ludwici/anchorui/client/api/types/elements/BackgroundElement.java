package com.ludwici.anchorui.client.api.types.elements;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class BackgroundElement {
    public int color;
    public ResourceLocation image;

    public BackgroundElement() {

    }

    public BackgroundElement(String color, ResourceLocation image) {
        this.color = Integer.parseInt(color, 16);
        this.image = image;
    }

    public BackgroundElement(JsonObject root) {
        this(
                GsonHelper.getAsString(root, "color", "FFFFFF"),
                ResourceLocation.tryParse(GsonHelper.getAsString(root, "image", ""))
        );
    }
}
