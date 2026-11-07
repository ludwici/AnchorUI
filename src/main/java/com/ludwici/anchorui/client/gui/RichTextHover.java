package com.ludwici.anchorui.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class RichTextHover {
    private RichTextHover() {
    }

    public static Style styleAt(Font font, FormattedCharSequence line, int offsetX) {
        int[] offset = {0};
        Style[] hovered = {null};
        line.accept((index, glyphStyle, codepoint) -> {
            int width = font.width(Component.literal(new String(Character.toChars(codepoint))).withStyle(glyphStyle));
            if (offsetX >= offset[0] && offsetX < offset[0] + width) {
                hovered[0] = glyphStyle;
                return false;
            }
            offset[0] += width;
            return true;
        });
        return hovered[0];
    }
}
