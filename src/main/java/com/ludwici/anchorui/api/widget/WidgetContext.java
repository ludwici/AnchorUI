package com.ludwici.anchorui.api.widget;

import com.ludwici.anchorui.api.LoreEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record WidgetContext(Minecraft minecraft, Screen screen, ItemStack item, LoreEntity entity,
                            WidgetInstanceRegistry widgets, ItemContext itemContext) {
    public WidgetContext(Minecraft minecraft, Screen screen, ItemStack item, LoreEntity entity) {
        this(minecraft, screen, item, entity, new WidgetInstanceRegistry(), null);
    }

    public WidgetContext(Minecraft minecraft, Screen screen, ItemStack item, LoreEntity entity, WidgetInstanceRegistry widgets) {
        this(minecraft, screen, item, entity, widgets, null);
    }

    public WidgetContext withItemContext(ItemContext context) {
        return new WidgetContext(minecraft, screen, item, entity, widgets, context);
    }

    public Font font() {
        return minecraft.font;
    }

    public ItemStack resolveItem(String id) {
        ResourceLocation location = id == null ? null : ResourceLocation.tryParse(id);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) {
            return item;
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(location));
    }
}
