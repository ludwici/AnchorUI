package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.GuiGraphics;

public final class EditorInspectorCheckbox {
    private EditorInspectorCheckbox() {
    }

    public static void render(GuiGraphics graphics, int x, int y, boolean checked) {
        graphics.fill(x + 2, y, x + 13, y + 11, 0xFF8FA5B7);
        graphics.fill(x + 3, y + 1, x + 12, y + 10, checked ? 0xFF377D70 : 0xFF223448);
        if (checked) {
            graphics.fill(x + 4, y + 5, x + 6, y + 7, 0xFFF5FFF9);
            graphics.fill(x + 6, y + 7, x + 8, y + 9, 0xFFF5FFF9);
            graphics.fill(x + 7, y + 5, x + 9, y + 7, 0xFFF5FFF9);
            graphics.fill(x + 9, y + 3, x + 11, y + 5, 0xFFF5FFF9);
        }
    }
}
