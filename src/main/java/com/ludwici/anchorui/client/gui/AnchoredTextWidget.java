package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public class AnchoredTextWidget extends AnchoredWidget {
    private final Font font;
    private final String text;

    public AnchoredTextWidget(String text, Font font, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : font.width(text) + style.padding.left + style.padding.right,
                style.size.height > 0 ? style.size.height : font.lineHeight + style.padding.top + style.padding.bottom, style);
        this.font = font;
        this.text = text;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        guiGraphics.drawString(font, text, getX() + style.padding.left, getY() + style.padding.top, style.textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
