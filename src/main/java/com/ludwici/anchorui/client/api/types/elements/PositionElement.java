package com.ludwici.anchorui.client.api.types.elements;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.client.api.types.ElementAnchor;
import net.minecraft.util.GsonHelper;

public class PositionElement {
    public ElementAnchor point = ElementAnchor.TOPLEFT;
    public ElementAnchor anchor = ElementAnchor.TOPLEFT;
    public int x = 0;
    public int y = 0;
    public String relativeTo = "";

    public PositionElement() {

    }

    public PositionElement(ElementAnchor point, ElementAnchor anchor, String relativeTo, int x, int y) {
        this.point = point;
        this.anchor = anchor;
        this.relativeTo = relativeTo;
        this.x = x;
        this.y = y;
    }

    public PositionElement(String point, String anchor, String relativeTo, int x, int y) {
        this(ElementAnchor.fromString(point), anchor.isEmpty() ? ElementAnchor.fromString(point) : ElementAnchor.fromString(anchor), relativeTo, x, y);
    }

    public PositionElement(JsonObject root) {
        this(
                GsonHelper.getAsString(root, "point", "TOPLEFT"),
                GsonHelper.getAsString(root, "anchor", ""),
                GsonHelper.getAsString(root, "relativeTo", "screen"),
                GsonHelper.getAsInt(root, "x", 0),
                GsonHelper.getAsInt(root, "y", 0)
        );
    }
}
