package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class EditorInspectorButton {
    public static final int SIZE = 11;
    public static final int HITBOX_WIDTH = 14;
    public static final int HITBOX_OFFSET = 2;

    private EditorInspectorButton() {
    }

    public static void draw(GuiGraphics graphics, Font font, String symbol, int x, int y,
                            int background, int foreground) {
        int left = x + HITBOX_OFFSET;
        graphics.fill(left, y, left + SIZE, y + SIZE, background);
        graphics.drawString(font, symbol, left + (SIZE - font.width(symbol)) / 2, y + 1, foreground, false);
    }

    public static void drawColor(GuiGraphics graphics, int x, int y, int argb) {
        int left = x + HITBOX_OFFSET;
        graphics.fill(left, y, left + SIZE, y + SIZE, 0xFF718699);
        graphics.fill(left + 1, y + 1, left + SIZE - 1, y + SIZE - 1, 0xFF273340);
        graphics.fill(left + 1, y + 1, left + SIZE - 1, y + SIZE - 1, argb);
    }

    public static boolean contains(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + HITBOX_WIDTH
                && mouseY >= y && mouseY < y + SIZE;
    }
}
