package com.ludwici.anchorui.api.style;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public class PaddingElement {
    public int left = 0;
    public int top = 0;
    public int right = 0;
    public int bottom = 0;

    public PaddingElement() {

    }

    public PaddingElement(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public PaddingElement(int horizontal, int vertical) {
        this(horizontal, vertical, horizontal, vertical);
    }

    public PaddingElement(JsonObject root) {
        this(
                GsonHelper.getAsInt(root, "left", 0),
                GsonHelper.getAsInt(root, "top", 0),
                GsonHelper.getAsInt(root, "right", 0),
                GsonHelper.getAsInt(root, "bottom", 0)
        );
    }
}
