package com.ludwici.anchorui.api.style;

import java.util.Locale;

public final class ColorValues {
    private ColorValues() {
    }

    public static int parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Color must be a string in #RRGGBB or #AARRGGBB format");
        }
        String hex = value.startsWith("#") ? value.substring(1) : value;
        if (!hex.matches("(?i)([0-9a-f]{6}|[0-9a-f]{8})")) {
            throw new IllegalArgumentException("Invalid color '" + value
                    + "'; expected #RRGGBB or #AARRGGBB");
        }
        long parsed = Long.parseLong(hex, 16);
        return (int) (hex.length() == 6 ? parsed | 0xFF000000L : parsed);
    }

    public static String format(int color) {
        return color >>> 24 == 0xFF
                ? String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF)
                : String.format(Locale.ROOT, "#%08X", color);
    }
}
