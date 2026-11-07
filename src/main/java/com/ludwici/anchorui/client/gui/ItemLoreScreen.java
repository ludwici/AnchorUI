package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.client.api.LoreComponents;
import com.ludwici.anchorui.client.api.types.LoreModel;
import com.ludwici.anchorui.client.api.types.LoreText;
import com.ludwici.anchorui.client.gui.widgets.LoreModelWidget;
import com.ludwici.anchorui.client.gui.widgets.LoreTextWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ItemLoreScreen extends Screen {

    public LoreComponents loreComponents;

    public ItemLoreScreen(LoreComponents loreComponents) {
        super(Component.empty());
        this.loreComponents = loreComponents;
    }

    @Override
    protected void init() {
        loreComponents.components.forEach(c -> {
            if (c instanceof LoreText lore) {
                addRenderableWidget(new LoreTextWidget(lore, minecraft.font, this));
            } else if (c instanceof LoreModel lore) {
//                addRenderableWidget(new LoreModelWidget());
            }
        });
    }



    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics) {
        super.renderBackground(pGuiGraphics);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
