package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.GuiGraphics;

public final class EditorInspectorCheckbox {
    private EditorInspectorCheckbox() {
    }

    public static void render(GuiGraphics graphics, int x, int y, boolean checked) {
        int left = x + EditorInspectorButton.HITBOX_OFFSET;
        int size = EditorInspectorButton.SIZE;
        graphics.fill(left, y, left + size, y + size, 0xFF718699);
        graphics.fill(left + 1, y + 1, left + size - 1, y + size - 1, checked ? 0xFF426D94 : 0xD923354A);
        if (checked) {
            int mark = 5;
            int inset = (size - mark) / 2;
            graphics.fill(left + inset, y + inset, left + inset + mark, y + inset + mark, 0xFFF1F4F7);
        }
    }
}
