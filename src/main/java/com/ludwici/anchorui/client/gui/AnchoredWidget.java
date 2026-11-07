package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public abstract class AnchoredWidget extends AbstractWidget {
    protected final LoreStyle.Element style;

    protected AnchoredWidget(int width, int height, LoreStyle.Element style) {
        super(0, 0, width, height, Component.empty());
        this.style = style;
        visible = !style.hidden;
    }

    protected void renderBackground(GuiGraphics guiGraphics) {
        if ((style.background.color >>> 24) != 0) {
            guiGraphics.fill(getX(), getY(), getRight(), getBottom(), style.background.color);
        }
    }
}
