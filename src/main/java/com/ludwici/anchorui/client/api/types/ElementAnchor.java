package com.ludwici.anchorui.client.api.types;

public enum ElementAnchor {
    TOPLEFT,
    TOP,
    TOPRIGHT,
    LEFT,
    CENTER,
    RIGHT,
    BOTTOMLEFT,
    BOTTOM,
    BOTTOMRIGHT;

    public static ElementAnchor fromString(String value) {
        return ElementAnchor.valueOf(value.toUpperCase());
    }
}
