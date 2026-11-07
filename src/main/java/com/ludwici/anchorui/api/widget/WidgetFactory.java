package com.ludwici.anchorui.api.widget;

import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.client.gui.components.AbstractWidget;

@FunctionalInterface
public interface WidgetFactory {
    AbstractWidget create(WidgetContext context, LoreEntity.UiElement element, LoreStyle.Element style);
}
