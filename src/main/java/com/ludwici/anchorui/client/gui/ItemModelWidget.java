package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.world.item.ItemStack;

public class ItemModelWidget extends AnchoredWidget {
    private final ItemStack stack;
    private final float scale;

    public ItemModelWidget(ItemStack stack, float scale, LoreStyle.Element style) {
        super(style.size.width > 0 ? style.size.width : 128, style.size.height > 0 ? style.size.height : 128, style);
        this.stack = stack;
        this.scale = scale;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        if (stack.isEmpty()) {
            return;
        }

        float availableWidth = Math.max(16, getWidth() - style.padding.left - style.padding.right);
        float availableHeight = Math.max(16, getHeight() - style.padding.top - style.padding.bottom);
        float renderScale = Math.max(0.1F, Math.min(scale, Math.min(availableWidth, availableHeight) / 16.0F));
        float centerX = getX() + style.padding.left + availableWidth / 2.0F;
        float centerY = getY() + style.padding.top + availableHeight / 2.0F;

        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 150);
        pose.scale(renderScale, renderScale, 1.0F);
        guiGraphics.renderItem(stack, -8, -8);
        pose.popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
