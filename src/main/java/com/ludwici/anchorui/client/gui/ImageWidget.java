package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;

public final class ImageWidget extends AnchoredWidget {
    private final TextureRegion texture;

    public ImageWidget(WidgetContext context, JsonObject settings, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : 64, style.size.height > 0 ? style.size.height : 64, style);
        texture = new TextureRegion(context, settings);
        active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        texture.render(graphics, getX() + style.padding.left, getY() + style.padding.top, getWidth() - style.padding.left - style.padding.right, getHeight() - style.padding.top - style.padding.bottom);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
