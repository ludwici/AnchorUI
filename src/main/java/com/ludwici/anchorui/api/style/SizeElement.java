package com.ludwici.anchorui.api.style;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public class SizeElement {
    public final int width;
    public final int height;

    public SizeElement(JsonObject root) {
        width = Math.max(0, GsonHelper.getAsInt(root, "width", 0));
        height = Math.max(0, GsonHelper.getAsInt(root, "height", 0));
    }
}
