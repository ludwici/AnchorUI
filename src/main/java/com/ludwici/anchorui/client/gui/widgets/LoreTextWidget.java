package com.ludwici.anchorui.client.gui.widgets;

import com.ludwici.anchorui.client.api.types.LoreText;
import com.ludwici.anchorui.client.gui.ItemLoreScreen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;

public class LoreTextWidget extends AbstractLoreWidget<LoreText> {

//    public LoreText base;

    public Font font;
    private int color = 16777215;

    public LoreTextWidget(LoreText base, Font font, ItemLoreScreen screen) {
        super(base, screen);
        this.font = font;
        if (base.size.width == 0) {
            setWidth(font.width(base.text) + base.getPaddingLeft() + base.getPaddingRight());
        } else {
            setWidth(base.size.width + base.getPaddingRight());
        }

        if (base.size.height == 0) {
            setHeight(font.lineHeight + base.getPaddingTop() + base.getPaddingBottom());
        } else {
            setHeight(base.size.height + base.getPaddingBottom());
        }
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (base.hidden) {
            return;
        }
//        if (base.background.image != null) {
//            pGuiGraphics.blit(base.background.image, getX(), getY(), 0, 0, getX() + getWidth(), getY() + getHeight());
//        }
//        pGuiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x80000000);
//        pGuiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), base.background.color);
        pGuiGraphics.drawString(font, base.text, getX() + base.getPaddingLeft(), getY() + base.getPaddingTop(), base.color, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
    }
}
