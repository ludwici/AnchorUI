package com.ludwici.anchorui.api.style;

import java.util.Locale;

public enum ElementAnchor {
    TOPLEFT(0, 0),
    TOP(1, 0),
    TOPRIGHT(2, 0),
    LEFT(0, 1),
    CENTER(1, 1),
    RIGHT(2, 1),
    BOTTOMLEFT(0, 2),
    BOTTOM(1, 2),
    BOTTOMRIGHT(2, 2);

    private final int horizontal;
    private final int vertical;

    ElementAnchor(int horizontal, int vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public int x(int width) {
        return width * horizontal / 2;
    }

    public int y(int height) {
        return height * vertical / 2;
    }

    public static ElementAnchor fromString(String value) {
        return ElementAnchor.valueOf(value.replace("_", "").toUpperCase(Locale.ROOT));
    }
}
