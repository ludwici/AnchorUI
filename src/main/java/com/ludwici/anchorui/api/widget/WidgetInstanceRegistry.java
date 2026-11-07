package com.ludwici.anchorui.api.widget;

import com.ludwici.anchorui.api.LoreEntity;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class WidgetInstanceRegistry {
    private final Map<String, LoreEntity.UiElement> definitions = new LinkedHashMap<>();
    private final Map<String, AbstractWidget> widgets = new LinkedHashMap<>();

    public void declare(String id, LoreEntity.UiElement element) {
        if (definitions.putIfAbsent(id, element) != null) {
            throw new IllegalArgumentException("Duplicate widget id: " + id);
        }
    }

    public void attach(String id, AbstractWidget widget) {
        if (!definitions.containsKey(id)) {
            throw new IllegalArgumentException("Undeclared widget id: " + id);
        }
        if (widgets.putIfAbsent(id, widget) != null) {
            throw new IllegalArgumentException("Widget instance already attached: " + id);
        }
    }

    public Optional<LoreEntity.UiElement> definition(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public Optional<AbstractWidget> widget(String id) {
        return Optional.ofNullable(widgets.get(id));
    }

    public Set<String> ids() {
        return Set.copyOf(definitions.keySet());
    }

    public Map<String, AbstractWidget> instances() {
        return Map.copyOf(widgets);
    }
}
