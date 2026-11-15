package com.ludwici.anchorui.client.gui;

import java.awt.Color;
import java.util.Locale;

public final class ColorPickerColor {
    private ColorPickerColor() {
    }

    public static int parse(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("#") || trimmed.matches("(?i)[0-9a-f]{6}|[0-9a-f]{8}")
                ? parseHex(trimmed) : parseRgb(trimmed, 0xFF);
    }

    public static int parseHex(String value) {
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (!hex.matches("(?i)[0-9a-f]{6}|[0-9a-f]{8}")) {
            throw new IllegalArgumentException("HEX must be #RRGGBB or #AARRGGBB");
        }
        long number = Long.parseLong(hex, 16);
        return (int) (hex.length() == 6 ? number | 0xFF000000L : number);
    }

    public static int parseRgb(String value, int alpha) {
        String source = value.trim();
        if (source.regionMatches(true, 0, "rgb(", 0, 4) && source.endsWith(")")) {
            source = source.substring(4, source.length() - 1);
        }
        String[] channels = source.split(",", -1);
        if (channels.length != 3) {
            throw new IllegalArgumentException("RGB must contain three channels: 0–255, 0–255, 0–255");
        }
        int[] rgb = new int[3];
        for (int index = 0; index < rgb.length; index++) {
            String channel = channels[index].trim();
            if (!channel.matches("[0-9]{1,3}")) {
                throw new IllegalArgumentException("RGB channels must be integers from 0 to 255");
            }
            rgb[index] = Integer.parseInt(channel);
            if (rgb[index] > 255) {
                throw new IllegalArgumentException("RGB channels must be integers from 0 to 255");
            }
        }
        return (alpha & 0xFF) << 24 | rgb[0] << 16 | rgb[1] << 8 | rgb[2];
    }

    public static String hex(int color) {
        return (color >>> 24) == 0xFF
                ? String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF)
                : String.format(Locale.ROOT, "#%08X", color);
    }

    public static String rgb(int color) {
        return (color >>> 16 & 0xFF) + ", " + (color >>> 8 & 0xFF) + ", " + (color & 0xFF);
    }

    public static float[] hsv(int color) {
        return Color.RGBtoHSB(color >>> 16 & 0xFF, color >>> 8 & 0xFF, color & 0xFF, null);
    }

    public static int hsvColor(float hue, float saturation, float brightness, int alpha) {
        return (alpha & 0xFF) << 24 | Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
    }
}
