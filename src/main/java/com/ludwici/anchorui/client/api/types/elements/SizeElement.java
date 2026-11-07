package com.ludwici.anchorui.client.api.types.elements;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public class SizeElement {
    public int width = 0;
    public int height = 0;

    public SizeElement() {

    }

    public SizeElement(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public SizeElement(JsonObject root) {
        this(
                GsonHelper.getAsInt(root, "width", 0),
                GsonHelper.getAsInt(root, "height", 0)
        );
    }
}
