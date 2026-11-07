package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.client.LoreLoader;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemLoreScreen extends Screen {
    private final Screen previous;
    private final ItemStack item;
    private final LoreEntity loreEntity;

    public ItemLoreScreen(Screen previous, ItemStack item, LoreEntity entity) {
        super(Component.empty());
        this.previous = previous;
        this.item = item;
        this.loreEntity = entity;
    }

    @Override
    protected void init() {
        LoreStyle theme = LoreLoader.INSTANCE.getStyle(loreEntity.style);
        List<AnchorLayout.Frame> frames = new ArrayList<>();
        List<AnchoredWidget> widgets = new ArrayList<>();
        if (loreEntity.entities.isEmpty()) {
            add("title", new AnchoredTextWidget(loreEntity.title, minecraft.font, theme.get("title", null)), theme.get("title", null), frames, widgets);
            add("model", new ItemModelWidget(item, loreEntity.scale, theme.get("model", null)), theme.get("model", null), frames, widgets);
            add("lore", new ItemLoreListWidget(loreEntity.components, minecraft.font, theme.get("lore", null)), theme.get("lore", null), frames, widgets);
        } else {
            int index = 0;
            for (LoreEntity.UiElement element : loreEntity.entities) {
                LoreStyle.Element settings = theme.get(element.type, element.settings);
                String id = element.id.isBlank() ? element.type + "_" + index : element.id;
                switch (element.type) {
                    case "text" -> add(id, new AnchoredTextWidget(element.text, minecraft.font, settings), settings, frames, widgets);
                    case "model" -> add(id, new ItemModelWidget(modelItem(element.path), element.scale, settings), settings, frames, widgets);
                    case "list" -> add(id, new ItemLoreListWidget(element.lore.isEmpty() ? loreEntity.components : element.lore, minecraft.font, settings), settings, frames, widgets);
                    default -> com.ludwici.anchorui.AnchorUI.LOGGER.warn("Unknown UI element type: {}", element.type);
                }
                index++;
            }
        }
        try {
            var positions = AnchorLayout.resolve(width, height, frames);
            for (int i = 0; i < frames.size(); i++) {
                var rect = positions.get(frames.get(i).id());
                widgets.get(i).setPosition(rect.x(), rect.y());
            }
        } catch (IllegalArgumentException ex) {
            com.ludwici.anchorui.AnchorUI.LOGGER.error("Invalid Anchor UI layout: {}", ex.getMessage());
        }
        widgets.forEach(this::addRenderableWidget);
    }

    private void add(String id, AnchoredWidget widget, LoreStyle.Element settings, List<AnchorLayout.Frame> frames, List<AnchoredWidget> widgets) {
        frames.add(new AnchorLayout.Frame(id, settings.position, widget.getWidth(), widget.getHeight()));
        widgets.add(widget);
    }

    private ItemStack modelItem(String path) {
        ResourceLocation location = ResourceLocation.tryParse(path);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) {
            return item;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(location));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
