package com.ludwici.anchorui.api.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.widget.WidgetContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class UiEventBindings {
    private final Map<String, Consumer<UiEvent>> handlers;
    private final Set<String> failed = new HashSet<>();

    private UiEventBindings(Map<String, Consumer<UiEvent>> handlers) {
        this.handlers = Map.copyOf(handlers);
    }

    public static UiEventBindings compile(WidgetContext context, JsonObject settings, String... names) {
        Map<String, Consumer<UiEvent>> handlers = new HashMap<>();
        for (String name : names) {
            JsonElement json = settings.get(name);
            if (json != null && !json.isJsonNull()) {
                handlers.put(name, UiActionRegistry.compileEvent(context, json));
            }
        }
        return new UiEventBindings(handlers);
    }

    public boolean has(String name) {
        return handlers.containsKey(name);
    }

    public void fire(String name, String targetId, JsonElement value) {
        Consumer<UiEvent> handler = handlers.get(name);
        if (handler == null) {
            return;
        }
        try {
            handler.accept(new UiEvent(name, targetId, value));
        } catch (RuntimeException exception) {
            if (failed.add(name)) {
                AnchorUI.LOGGER.error("Anchor UI event '{}' failed for '{}'", name, targetId, exception);
            }
        }
    }
}
