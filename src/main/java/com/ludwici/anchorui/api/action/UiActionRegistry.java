package com.ludwici.anchorui.api.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class UiActionRegistry {
    @FunctionalInterface
    public interface Factory {
        Runnable create(WidgetContext context, JsonObject settings);
    }

    @FunctionalInterface
    public interface EventFactory {
        Consumer<UiEvent> create(WidgetContext context, JsonObject settings);
    }

    private static final Map<ResourceLocation, EventFactory> FACTORIES = new ConcurrentHashMap<>();

    private UiActionRegistry() {
    }

    public static void register(ResourceLocation id, Factory factory) {
        Objects.requireNonNull(factory, "Action factory must not be null");
        registerEvent(id, (context, settings) -> {
            Runnable action = Objects.requireNonNull(factory.create(context, settings), "Action factory returned null: " + id);
            return event -> action.run();
        });
    }

    public static void registerEvent(ResourceLocation id, EventFactory factory) {
        Objects.requireNonNull(id, "Action identifier must not be null");
        Objects.requireNonNull(factory, "Event action factory must not be null");
        if (FACTORIES.putIfAbsent(id, factory) != null) {
            throw new IllegalStateException("Anchor UI action already registered: " + id);
        }
    }

    public static Runnable compile(WidgetContext context, JsonElement json) {
        Consumer<UiEvent> action = compileEvent(context, json);
        return () -> action.accept(UiEvent.of("onClick", ""));
    }

    public static Consumer<UiEvent> compileEvent(WidgetContext context, JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return event -> {};
        }
        if (!json.isJsonObject()) {
            throw new IllegalArgumentException("UI action must be a JSON object");
        }
        JsonObject object = json.getAsJsonObject();
        JsonElement type = object.get("type");
        if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("UI action type must be a namespaced identifier");
        }
        String rawId = type.getAsString();
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null || !rawId.contains(":")) {
            throw new IllegalArgumentException("Invalid UI action id: " + rawId);
        }
        EventFactory factory = FACTORIES.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown UI action type: " + id);
        }
        return Objects.requireNonNull(factory.create(context, object), "Event action factory returned null: " + id);
    }
}
