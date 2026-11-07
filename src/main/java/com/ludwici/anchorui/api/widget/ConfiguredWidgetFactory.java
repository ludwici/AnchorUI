package com.ludwici.anchorui.api.widget;

import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.client.gui.components.AbstractWidget;

@FunctionalInterface
public interface ConfiguredWidgetFactory<T> {
    AbstractWidget create(WidgetContext context, LoreEntity.UiElement element, LoreStyle.Element style, T config);
}
