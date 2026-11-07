package com.ludwici.anchorui.api.style;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public class PositionElement {
    public final ElementAnchor point;
    public final ElementAnchor anchor;
    public final String relativeTo;
    public final int x;
    public final int y;

    public PositionElement(ElementAnchor point, ElementAnchor anchor, String relativeTo, int x, int y) {
        this.point = point;
        this.anchor = anchor;
        this.relativeTo = relativeTo == null || relativeTo.isBlank() ? "screen" : relativeTo;
        this.x = x;
        this.y = y;
    }

    public PositionElement(JsonObject root) {
        this(
                ElementAnchor.fromString(GsonHelper.getAsString(root, "point", GsonHelper.getAsString(root, "anchor", "TOPLEFT"))),
                ElementAnchor.fromString(GsonHelper.getAsString(root, "anchor", GsonHelper.getAsString(root, "point", "TOPLEFT"))),
                GsonHelper.getAsString(root, "relativeTo", "screen"),
                GsonHelper.getAsInt(root, "x", 0),
                GsonHelper.getAsInt(root, "y", 0)
        );
    }
}
