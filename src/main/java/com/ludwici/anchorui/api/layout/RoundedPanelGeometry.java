package com.ludwici.anchorui.api.layout;

import java.util.ArrayList;
import java.util.List;

public final class RoundedPanelGeometry {
    public record Band(int top, int height, int outerInset, int innerInset, boolean hasInterior) {
    }

    private RoundedPanelGeometry() {
    }

    public static List<Band> bands(int width, int height, int radius, int borderWidth) {
        if (width <= 0 || height <= 0 || radius < 0 || borderWidth < 0) {
            throw new IllegalArgumentException("Rounded panel dimensions and radii must be positive");
        }

        int border = Math.min(borderWidth, Math.min(width, height) / 2);
        int outerRadius = Math.min(radius, Math.min(width, height) / 2);
        int innerWidth = width - border * 2;
        int innerHeight = height - border * 2;
        int innerRadius = Math.max(0, outerRadius - border);
        innerRadius = Math.min(innerRadius, Math.min(innerWidth, innerHeight) / 2);
        List<Band> result = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            int outside = inset(row, height, outerRadius);
            int innerRow = row - border;
            boolean inside = innerWidth > 0 && innerHeight > 0 && innerRow >= 0 && innerRow < innerHeight;
            int innerInset = inside ? inset(innerRow, innerHeight, innerRadius) : 0;
            int last = result.size() - 1;
            if (last >= 0) {
                Band previous = result.get(last);
                if (previous.outerInset() == outside && previous.innerInset() == innerInset
                        && previous.hasInterior() == inside) {
                    result.set(last, new Band(previous.top(), previous.height() + 1, outside, innerInset, inside));
                    continue;
                }
            }
            result.add(new Band(row, 1, outside, innerInset, inside));
        }
        return List.copyOf(result);
    }

    private static int inset(int row, int height, int radius) {
        if (radius <= 0) {
            return 0;
        }
        double distance = Math.max(radius - row - 0.5, row + 0.5 - (height - radius));
        if (distance <= 0) {
            return 0;
        }
        return (int) Math.ceil(radius - Math.sqrt(Math.max(0, radius * radius - distance * distance)));
    }
}
