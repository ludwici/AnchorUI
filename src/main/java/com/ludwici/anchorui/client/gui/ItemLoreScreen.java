package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.LoreEntity;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemLoreScreen extends Screen {
    public final Screen screen;
    public ItemStack item;
    public LoreEntity loreEntity;

    //TODO: debug info about current file name with full path

    public ItemLoreScreen(Screen screen, ItemStack item, LoreEntity entity) {
        super(Component.empty());
        this.screen = screen;
        this.item = item;
        this.loreEntity = entity;
    }

    @Override
    protected void init() {
        addRenderableWidget(new ItemModelWidget((this.width) / 4, (this.height) / 4, this));
        addRenderableWidget(new ItemLoreListWidget( 20, 0, this));
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int centerX = (this.width) / 2;
        int centerY = (this.height) / 2;
        String title = item.getDisplayName().getString();
        int w = minecraft.font.width(loreEntity.title) / 2;
//        guiGraphics.drawString(minecraft.font, item.getDisplayName().getString(), centerX - w, 10, 0xFFE278, true);
        guiGraphics.drawString(minecraft.font, loreEntity.title, centerX - w, 10, 0xFFE278, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
