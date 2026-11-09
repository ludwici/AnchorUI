package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.action.UiActionRegistry;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class JsonActionButton {
    private JsonActionButton() {
    }

    public static Button create(WidgetContext context, LoreEntity.UiElement element, LoreStyle.Element style) {
        Runnable action = UiActionRegistry.compile(context, element.settings.get("onClick"));
        int width = style.size.width > 0 ? style.size.width : 120;
        int height = style.size.height > 0 ? style.size.height : 20;
        return Button.builder(Component.literal(element.text), button -> action.run()).bounds(0, 0, width, height).build();
    }
}
