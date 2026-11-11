package com.ludwici.anchorui.client.action;

import com.google.gson.JsonElement;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.action.UiActionRegistry;
import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.ScreenIds;
import com.ludwici.anchorui.client.gui.ItemLoreScreen;
import net.minecraft.resources.ResourceLocation;

public final class ClientUiActions {
    private ClientUiActions() {
    }

    public static void register() {
        UiActionRegistry.register(ResourceLocation.fromNamespaceAndPath("anchorui", "open_screen"), (context, config) -> {
            JsonElement screen = config.get("screen");
            if (screen == null || !screen.isJsonPrimitive() || !screen.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("open_screen requires a string 'screen' resource id");
            }
            String rawId = screen.getAsString();
            ResourceLocation id = ScreenIds.resolve(rawId);
            if (id == null) {
                throw new IllegalArgumentException("open_screen requires namespace:name");
            }
            return () -> {
                if (context.screen() == null || context.minecraft().screen != context.screen()) {
                    return;
                }
                var doc = LoreLoader.INSTANCE.getData().get(id);
                if (doc == null) {
                    AnchorUI.LOGGER.warn("Anchor UI screen '{}' is unavailable", id);
                    return;
                }
                context.minecraft().setScreen(new ItemLoreScreen(context.screen(), context.item(), doc, id));
            };
        });

        UiActionRegistry.register(ResourceLocation.fromNamespaceAndPath("anchorui", "close_screen"), (context, config) -> () -> {
            if (context.screen() != null && context.minecraft().screen == context.screen()) {
                context.screen().onClose();
            }
        });
    }
}
