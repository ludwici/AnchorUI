package com.ludwici.anchorui.client.gui;

import net.minecraft.client.gui.GuiGraphics;

public final class ControlVisualStates {
    private ControlVisualStates() {
    }

    public static String[] background(boolean active, boolean hovered, boolean focused) {
        if (!active) {
            return new String[]{"disabled", "normal"};
        }
        if (focused && hovered) {
            return new String[]{"hoverFocused", "focused", "hover", "normal"};
        }
        if (focused) {
            return new String[]{"focused", "hover", "normal"};
        }
        return hovered ? new String[]{"hover", "normal"} : new String[]{"normal"};
    }

    public static boolean needsFocusOutline(StateControlTextures textures, boolean active,
                                            boolean hovered, boolean focused) {
        return active && focused && !textures.hasVariant("background", "focused")
                && !(hovered && textures.hasVariant("background", "hoverFocused"));
    }

    public static void renderFocusOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width < 2 || height < 2) {
            return;
        }
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
