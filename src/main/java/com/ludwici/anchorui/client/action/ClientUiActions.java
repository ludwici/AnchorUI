package com.ludwici.anchorui.client.action;

import com.google.gson.JsonElement;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.action.UiActionRegistry;
import com.ludwici.anchorui.api.state.UiLocalState;
import com.ludwici.anchorui.api.state.UiStateHandoff;
import net.minecraft.network.chat.Component;

import java.util.Map;
import com.ludwici.anchorui.api.state.UiStateHost;
import com.ludwici.anchorui.api.widget.WidgetContext;
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
            Map<String, String> handoff;
            if (config.has("stateHandoff")) {
                handoff = UiStateHandoff.parse(config, screenState(context));
            } else {
                handoff = Map.of();
            }
            return () -> {
                if (context.screen() == null || context.minecraft().screen != context.screen()) {
                    return;
                }
                var doc = LoreLoader.INSTANCE.getData().get(id);
                if (doc == null) {
                    navigationError(context, "Unknown target screen '" + ScreenIds.display(id) + "'");
                    return;
                }
                try {
                    ItemLoreScreen destination = new ItemLoreScreen(context.screen(), context.item(), doc, id);
                    if (!handoff.isEmpty()) {
                        UiStateHandoff.copy(screenState(context), destination.uiState(), handoff);
                    }
                    context.minecraft().setScreen(destination);
                } catch (IllegalArgumentException exception) {
                    navigationError(context, "Cannot open '" + ScreenIds.display(id) + "': " + exception.getMessage());
                }
            };
        });

        UiActionRegistry.register(ResourceLocation.fromNamespaceAndPath("anchorui", "close_screen"), (context, config) -> () -> {
            if (context.screen() != null && context.minecraft().screen == context.screen()) {
                context.screen().onClose();
            }
        });

        UiActionRegistry.register(ResourceLocation.fromNamespaceAndPath("anchorui", "set_state"), (context, config) -> {
            UiLocalState state = screenState(context);
            String key = stateKey(config);
            JsonElement raw = config.get("value");
            state.validateSet(key, raw);
            JsonElement value = raw.deepCopy();
            return () -> {
                if (context.minecraft().screen == context.screen()) {
                    state.set(key, value);
                }
            };
        });

        UiActionRegistry.register(ResourceLocation.fromNamespaceAndPath("anchorui", "toggle_state"), (context, config) -> {
            UiLocalState state = screenState(context);
            String key = stateKey(config);
            state.validateToggle(key);
            return () -> {
                if (context.minecraft().screen == context.screen()) {
                    state.toggle(key);
                }
            };
        });
    }

    private static void navigationError(WidgetContext context, String message) {
        AnchorUI.LOGGER.warn("Anchor UI navigation: {}", message);
        if (context.minecraft().player != null) {
            context.minecraft().player.displayClientMessage(Component.literal("AnchorUI: " + message), false);
        }
    }

    private static UiLocalState screenState(WidgetContext context) {
        if (!(context.screen() instanceof UiStateHost host)) {
            throw new IllegalArgumentException("UI state actions can only be used in screens with local UI state");
        }
        return host.uiState();
    }

    private static String stateKey(com.google.gson.JsonObject config) {
        JsonElement raw = config.get("key");
        if (raw == null || !raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("UI state action requires a string 'key'");
        }
        return raw.getAsString();
    }
}
