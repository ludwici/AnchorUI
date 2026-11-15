package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.style.ColorValues;

public final class WidgetColors {
    private WidgetColors() {
    }

    public static int parse(String value) {
        return ColorValues.parse(value);
    }
}
