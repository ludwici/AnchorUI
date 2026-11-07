package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.LoreComponent;
import com.ludwici.anchorui.api.LoreParser;
import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class ItemLoreListWidget extends AnchoredWidget {
    private final Font font;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private final int lineHeight;

    public ItemLoreListWidget(List<LoreComponent> content, Font font, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : 160, style.size.height > 0 ? style.size.height : 1, style);
        this.font = font;
        lineHeight = font.lineHeight + 2;
        int availableWidth = Math.max(1, getWidth() - style.padding.left - style.padding.right);
        for (LoreComponent entry : content) {
            if (entry.hidden) {
                lines.addAll(font.split(Component.literal("??????"), availableWidth));
            } else {
                lines.addAll(font.split(LoreParser.toComponent(entry.pieces), availableWidth));
            }
            lines.add(null);
        }
        if (!lines.isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        if (style.size.height == 0) {
            int lineCount = 0;
            int gaps = 0;
            for (FormattedCharSequence line : lines) {
                if (line == null) {
                    gaps++;
                } else {
                    lineCount++;
                }
            }
            setHeight(style.padding.top + style.padding.bottom + lineCount * lineHeight + gaps * 6);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        int x = getX() + style.padding.left;
        int y = getY() + style.padding.top;
        for (FormattedCharSequence line : lines) {
            if (line == null) {
                y += 6;
                continue;
            }
            if (y + font.lineHeight > getBottom()) {
                break;
            }
            guiGraphics.drawString(font, line, x, y, style.textColor, false);
            if (mouseX >= x && mouseX < x + font.width(line) && mouseY >= y && mouseY < y + font.lineHeight) {
                var hovered = RichTextHover.styleAt(font, line, mouseX - x);
                if (hovered != null && hovered.getHoverEvent() != null) {
                    guiGraphics.renderComponentHoverEffect(font, hovered, mouseX, mouseY);
                }
            }
            y += lineHeight;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
