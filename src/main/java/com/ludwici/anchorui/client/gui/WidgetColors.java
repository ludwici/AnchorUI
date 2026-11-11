package com.ludwici.anchorui.client.gui;

public final class WidgetColors {
    private WidgetColors() {
    }

    public static int parse(String value) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (hex.length() != 6 && hex.length() != 8) {
            throw new IllegalArgumentException("Expected color #RRGGBB or #AARRGGBB: " + value);
        }
        try {
            long parsed = Long.parseLong(hex, 16);
            return (int) (hex.length() == 6 ? parsed | 0xFF000000L : parsed);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid color: " + value, exception);
        }
    }
}
