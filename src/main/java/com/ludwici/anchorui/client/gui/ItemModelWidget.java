package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.style.LoreStyle;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.world.item.ItemStack;

public class ItemModelWidget extends AnchoredWidget {
    private final ItemStack stack;
    private final float scale;
    private float rotationX;
    private float rotationY = 45;

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
        var pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(getX() + getWidth() / 2.0F, getY() + getHeight() / 2.0F, 150);
        pose.mulPose(Axis.XP.rotationDegrees(rotationX));
        pose.mulPose(Axis.YP.rotationDegrees(rotationY));
        pose.scale(scale, scale, scale);
        guiGraphics.renderItem(stack, -8, -8);
        pose.popPose();
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        rotationX = (rotationX - (float) dragY * 2.5F) % 360.0F;
        rotationY = (rotationY + (float) dragX * 2.5F) % 360.0F;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
