package com.ludwici.anchorui.api.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.condition.BoundCondition;
import com.ludwici.anchorui.api.widget.WidgetConfigDecoder;
import com.ludwici.anchorui.api.widget.WidgetContext;
import com.mojang.serialization.Codec;
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

    @FunctionalInterface
    public interface ConfiguredFactory<T> {
        Runnable create(WidgetContext context, T arguments);
    }

    @FunctionalInterface
    public interface ConfiguredEventFactory<T> {
        Consumer<UiEvent> create(WidgetContext context, T arguments);
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

    /**
     * Registers a legacy Runnable action with typed, Codec-validated JSON arguments.
     * The reserved JSON field 'type' is not passed to the codec.
     */
    public static <T> void register(ResourceLocation id, Codec<T> codec, ConfiguredFactory<T> factory) {
        Objects.requireNonNull(factory, "Configured action factory must not be null");
        registerEvent(id, codec, (context, arguments) -> {
            Runnable action = Objects.requireNonNull(factory.create(context, arguments), "Configured action factory returned null: " + id);
            return event -> action.run();
        });
    }

    /**
     * Registers an event-aware action with a typed, Codec-validated argument object.
     * Required fields, defaults, and nested values are described by the codec.
     */
    public static <T> void registerEvent(ResourceLocation id, Codec<T> codec, ConfiguredEventFactory<T> factory) {
        Objects.requireNonNull(codec, "Action argument codec must not be null");
        Objects.requireNonNull(factory, "Configured event action factory must not be null");
        registerEvent(id, (context, settings) -> {
            JsonObject arguments = settings.deepCopy();
            arguments.remove("type");
            T decoded;
            try {
                decoded = WidgetConfigDecoder.decode(codec, arguments);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid arguments for UI action '" + id + "': " + exception.getMessage(), exception);
            }
            return Objects.requireNonNull(factory.create(context, decoded), "Configured event action factory returned null: " + id);
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
        JsonObject object;
        if (json.isJsonObject()) {
            object = json.getAsJsonObject().deepCopy();
        } else if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            object = new JsonObject();
            object.addProperty("type", json.getAsString());
        } else {
            throw new IllegalArgumentException("UI action must be a namespaced string or a JSON object");
        }
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
        BoundCondition guard = object.has("when") ? BoundCondition.parse(object, "when", false, false) : null;
        object.remove("when");
        Consumer<UiEvent> action = Objects.requireNonNull(factory.create(context, object),
                "Event action factory returned null: " + id);
        return guard == null ? action : event -> {
            if (guard.get(context)) {
                action.accept(event);
            }
        };
    }
}
