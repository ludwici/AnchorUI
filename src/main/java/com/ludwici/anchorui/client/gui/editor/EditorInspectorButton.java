package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class EditorInspectorButton {
    public static final int SIZE = 11;
    public static final int HITBOX_WIDTH = 14;
    public static final int HITBOX_OFFSET = 2;

    public static int anchorMinusX(int panelRight) {
        return panelRight - 43;
    }

    public static int anchorPlusX(int panelRight) {
        return panelRight - 25;
    }

    private EditorInspectorButton() {
    }

    public static void draw(GuiGraphics graphics, Font font, String symbol, int x, int y,
                            int background, int foreground) {
        drawSquare(graphics, font, symbol, x + HITBOX_OFFSET, y, background, foreground);
    }

    public static void drawSquare(GuiGraphics graphics, Font font, String symbol, int x, int y,
                                  int background, int foreground) {
        graphics.fill(x, y, x + SIZE, y + SIZE, background);
        int textX = x + (SIZE - font.width(symbol)) / 2;
        int textY = y + (SIZE - font.lineHeight) / 2 + ("▼".equals(symbol) ? 1 : 0);
        graphics.drawString(font, symbol, textX, textY, foreground, true);
    }

    public static int centeredInRow(int rowY, int rowHeight) {
        return rowY + (rowHeight - SIZE) / 2;
    }

    public static float centeredTextInRow(int rowY, int rowHeight, int fontHeight, float scale) {
        return rowY + (rowHeight - (fontHeight - 1) * scale) / 2.0F;
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
