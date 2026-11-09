package com.ludwici.anchorui.api.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class UiActionRegistry {
    @FunctionalInterface
    public interface Factory {
        Runnable create(WidgetContext context, JsonObject settings);
    }

    private static final Map<ResourceLocation, Factory> FACTORIES = new ConcurrentHashMap<>();

    private UiActionRegistry() {
    }

    public static void register(ResourceLocation id, Factory factory) {
        Objects.requireNonNull(id, "Action identifier must not be null");
        Objects.requireNonNull(factory, "Action factory must not be null");
        if (FACTORIES.putIfAbsent(id, factory) != null) {
            throw new IllegalStateException("Anchor UI action already registered: " + id);
        }
    }

    public static Runnable compile(WidgetContext context, JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return () -> {};
        }
        if (!json.isJsonObject()) {
            throw new IllegalArgumentException("onClick must be a JSON object");
        }
        JsonObject object = json.getAsJsonObject();
        JsonElement type = object.get("type");
        if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("onClick.type must be a namespaced action identifier");
        }
        String rawId = type.getAsString();
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null || !rawId.contains(":")) {
            throw new IllegalArgumentException("Invalid onClick action id: " + rawId);
        }
        Factory factory = FACTORIES.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown onClick action type: " + id);
        }
        return factory.create(context, object);
    }
}
