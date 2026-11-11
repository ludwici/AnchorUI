package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.widget.WidgetConfigDecoder;
import com.ludwici.anchorui.api.widget.WidgetContext;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class DataFormatterRegistry {
    @FunctionalInterface
    public interface Formatter {
        JsonElement format(JsonElement value, WidgetContext context);
    }

    @FunctionalInterface
    public interface Factory {
        Formatter create(JsonObject config);
    }

    @FunctionalInterface
    public interface ConfiguredFormatter<T> {
        JsonElement format(JsonElement value, WidgetContext context, T config);
    }

    private static final Map<ResourceLocation, Factory> FACTORIES = new ConcurrentHashMap<>();

    private DataFormatterRegistry() {
    }

    public static void register(ResourceLocation id, Formatter formatter) {
        Objects.requireNonNull(formatter, "Data formatter must not be null");
        registerFactory(id, config -> formatter);
    }

    public static <T> void register(ResourceLocation id, Codec<T> codec, ConfiguredFormatter<T> formatter) {
        Objects.requireNonNull(codec, "Data formatter configuration codec must not be null");
        Objects.requireNonNull(formatter, "Configured data formatter must not be null");
        registerFactory(id, config -> {
            T options = WidgetConfigDecoder.decode(codec, config);
            return (value, context) -> formatter.format(value, context, options);
        });
    }

    public static void registerFactory(ResourceLocation id, Factory factory) {
        Objects.requireNonNull(id, "Data formatter identifier must not be null");
        Objects.requireNonNull(factory, "Data formatter factory must not be null");
        if (FACTORIES.putIfAbsent(id, factory) != null) {
            throw new IllegalStateException("Anchor UI data formatter already registered: " + id);
        }
    }

    public static Formatter compile(JsonElement format) {
        if (format == null || format.isJsonNull()) {
            return null;
        }

        String rawId;
        JsonObject config;
        if (format.isJsonPrimitive() && format.getAsJsonPrimitive().isString()) {
            rawId = format.getAsString();
            config = new JsonObject();
        } else if (format.isJsonObject()) {
            config = format.getAsJsonObject().deepCopy();
            JsonElement type = config.get("type");
            if (type == null || !type.isJsonPrimitive() || !type.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Formatter 'type' must be a namespaced string");
            }
            rawId = type.getAsString();
        } else {
            throw new IllegalArgumentException("Formatter must be a namespaced string or a configuration object");
        }

        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null || !rawId.contains(":")) {
            throw new IllegalArgumentException("Invalid namespaced formatter id: " + rawId);
        }
        Factory factory = FACTORIES.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown data formatter: " + id);
        }
        return Objects.requireNonNull(factory.create(config), "Data formatter factory returned null: " + id);
    }
}
