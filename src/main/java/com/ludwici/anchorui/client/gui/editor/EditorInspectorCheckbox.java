package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.GuiGraphics;

public final class EditorInspectorCheckbox {
    private EditorInspectorCheckbox() {
    }

    public static void render(GuiGraphics graphics, int x, int y, boolean checked) {
        graphics.fill(x + 2, y, x + 13, y + 11, 0xFF718699);
        graphics.fill(x + 3, y + 1, x + 12, y + 10, checked ? 0xFF426D94 : 0xD923354A);
        if (checked) {
            graphics.fill(x + 5, y + 3, x + 10, y + 8, 0xFFF1F4F7);
        }
    }
}
