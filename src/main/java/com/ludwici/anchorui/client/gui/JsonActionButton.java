package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.action.UiActionRegistry;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public final class JsonActionButton {
    private JsonActionButton() {
    }

    public static AbstractWidget create(WidgetContext context, LoreEntity.UiElement element, LoreStyle.Element style) {
        Runnable fallback = context.screen() instanceof ItemLoreScreen
                ? () -> {}
                : UiActionRegistry.compile(context, element.settings.get("onClick"));
        var settings = style.resolvedSettings;
        if (settings.has("textures") || settings.has("fillColor") || settings.has("borderColor")
                || settings.has("hoverColor") || settings.has("disabledColor") || settings.has("color")) {
            return new SkinnedActionButton(context, element.text, style, settings, button -> {
                if (context.screen() instanceof ItemLoreScreen screen) {
                    screen.dispatchWidgetClick(button);
                } else {
                    fallback.run();
                }
            });
        }
        int width = style.size.width > 0 ? style.size.width : 120;
        int height = style.size.height > 0 ? style.size.height : 20;
        return Button.builder(Component.literal(element.text), button -> {
            if (context.screen() instanceof ItemLoreScreen screen) {
                screen.dispatchWidgetClick(button);
            } else {
                fallback.run();
            }
        }).bounds(0, 0, width, height).build();
    }
}
