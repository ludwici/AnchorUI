package com.ludwici.anchorui.api.style;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public class PositionElement {
    public ElementAnchor anchor = ElementAnchor.CENTER;
    public int x = 0;
    public int y = 0;

    public PositionElement() {

    }

    public PositionElement(ElementAnchor anchor, int x, int y) {
        this.anchor = anchor;
        this.x = x;
        this.y = y;
    }

    public PositionElement(String anchor, int x, int y) {
        this(ElementAnchor.fromString(anchor), x, y);
    }

    public PositionElement(JsonObject root) {
        this(
                GsonHelper.getAsString(root, "anchor", "CENTER"),
                GsonHelper.getAsInt(root, "x", 0),
                GsonHelper.getAsInt(root, "y", 0)
        );
    }
}
