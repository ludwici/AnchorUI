package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class EditorInspectorButton {
    public static final int SIZE = 11;
    public static final int HITBOX_WIDTH = 14;
    public static final int HITBOX_OFFSET = 2;
    private static final int HOVER_BACKGROUND = 0xFF597B91;
    private static final int HOVER_BORDER = 0xFF91C8DC;

    public static int anchorMinusX(int panelRight) {
        return panelRight - 43;
    }

    public static int anchorPlusX(int panelRight) {
        return panelRight - 25;
    }

    private EditorInspectorButton() {
    }

    public static void draw(GuiGraphics graphics, Font font, String symbol, int x, int y,
                            int background, int foreground, int mouseX, int mouseY) {
        draw(graphics, font, symbol, x, y, background, foreground, mouseX, mouseY, true);
    }

    public static void draw(GuiGraphics graphics, Font font, String symbol, int x, int y,
                            int background, int foreground, int mouseX, int mouseY, boolean enabled) {
        boolean hovered = enabled && contains(mouseX, mouseY, x, y);
        renderSquare(graphics, font, symbol, x + HITBOX_OFFSET, y, background, foreground, hovered);
    }

    public static void drawSquare(GuiGraphics graphics, Font font, String symbol, int x, int y,
                                  int background, int foreground, int mouseX, int mouseY, boolean enabled) {
        boolean hovered = enabled && mouseX >= x && mouseX < x + SIZE && mouseY >= y && mouseY < y + SIZE;
        renderSquare(graphics, font, symbol, x, y, background, foreground, hovered);
    }

    private static void renderSquare(GuiGraphics graphics, Font font, String symbol, int x, int y,
                                     int background, int foreground, boolean hovered) {
        graphics.fill(x, y, x + SIZE, y + SIZE, hovered ? HOVER_BACKGROUND : background);
        if (hovered) {
            graphics.fill(x, y, x + SIZE, y + 1, HOVER_BORDER);
            graphics.fill(x, y + SIZE - 1, x + SIZE, y + SIZE, HOVER_BORDER);
            graphics.fill(x, y + 1, x + 1, y + SIZE - 1, HOVER_BORDER);
            graphics.fill(x + SIZE - 1, y + 1, x + SIZE, y + SIZE - 1, HOVER_BORDER);
        }
        int textX = x + (SIZE - font.width(symbol)) / 2 + 1;
        int textY = y + (SIZE - font.lineHeight) / 2 + 1 + ("▼".equals(symbol) ? 1 : 0);
        graphics.drawString(font, symbol, textX, textY, hovered ? 0xFFFFFFFF : foreground, true);
    }

    public static void drawHoverBorder(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SIZE, y + 1, HOVER_BORDER);
        graphics.fill(x, y + SIZE - 1, x + SIZE, y + SIZE, HOVER_BORDER);
        graphics.fill(x, y + 1, x + 1, y + SIZE - 1, HOVER_BORDER);
        graphics.fill(x + SIZE - 1, y + 1, x + SIZE, y + SIZE - 1, HOVER_BORDER);
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
