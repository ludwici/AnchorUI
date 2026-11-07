package com.ludwici.anchorui.client.gui;

import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ItemModelWidget extends AbstractWidget {

    private ItemLoreScreen screen;
    private float rotationX;
    private float rotationY;

    public ItemModelWidget(int x, int y, ItemLoreScreen screen) {
        super(x, y, screen.width / 2, screen.height / 2, Component.empty());
        this.screen = screen;
        this.rotationX = 0;
        this.rotationY = 45;
    }

    private void renderModel(GuiGraphics guiGraphics) {
        ItemStack stack = this.screen.item;

        if (stack.isEmpty()) {
            return;
        }

//        Minecraft minecraft = Minecraft.getInstance();
//        ResourceManager manager = minecraft.getResourceManager();
//        BlockModel blockModel;
//        BlockItemStateProperties blockItemStateProperties = (BlockItemStateProperties) stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
//        if (blockItemStateProperties.isEmpty()) {
//            guiGraphics.renderItem(stack, 0, 0);
//        } else {
//            BlockState blockState = blockItemStateProperties.
//        }
//        BlockState state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).;
//        BakedModel bakedModel = minecraft.getBlockRenderer().getBlockModel();

//        this.screen.getMinecraft().getBlockRenderer().getBlockModel(new BlockState())
//        Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(this.resourceLocation);
        guiGraphics.renderItem(stack, 0, 0);
//        this.screen.getMinecraft().getBlockRenderer().getBlockModel()
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        float scale = 4.0F;
        poseStack.translate(width, height, 10);
        poseStack.rotateAround(Axis.XP.rotationDegrees(this.rotationX), 0.0f, 0.0f, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(this.rotationY));
        poseStack.translate(-8 * scale, -8 * scale, -150 * scale);
        poseStack.scale(scale, scale, scale);
        renderModel(guiGraphics);
        poseStack.popPose();

//        if (isHovered()) {
//            guiGraphics.fill(getX(), getY(), getRight(), getBottom(), 0x80FFFFFF);
//        } else {
//            guiGraphics.fill(getX(), getY(), getRight(), getBottom(), -10,0x80000000);
//        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        this.rotationX = (this.rotationX - (float)dragY * 2.5F) % 360.0F;
        this.rotationY += (float)dragX * 2.5F;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
