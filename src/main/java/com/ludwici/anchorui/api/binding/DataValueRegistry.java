package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class DataValueRegistry {
    @FunctionalInterface
    public interface Provider {
        JsonElement read(WidgetContext context);
    }

    public static final class MutableValue implements Provider {
        private final AtomicReference<JsonElement> current;

        private MutableValue(JsonElement initial) {
            current = new AtomicReference<>(copy(initial));
        }

        public void set(JsonElement updated) {
            current.set(copy(updated));
        }

        @Override
        public JsonElement read(WidgetContext context) {
            return current.get().deepCopy();
        }
    }

    private static final Map<ResourceLocation, Provider> PROVIDERS = new ConcurrentHashMap<>();

    private DataValueRegistry() {
    }

    public static void register(ResourceLocation id, Provider provider) {
        Objects.requireNonNull(id, "Data source id must not be null");
        Objects.requireNonNull(provider, "Data source provider must not be null");
        if (PROVIDERS.putIfAbsent(id, provider) != null) {
            throw new IllegalStateException("Anchor UI data source already registered: " + id);
        }
    }

    public static Optional<Provider> find(ResourceLocation id) {
        return Optional.ofNullable(PROVIDERS.get(id));
    }

    public static MutableValue registerMutable(ResourceLocation id, JsonElement initial) {
        MutableValue provider = new MutableValue(initial);
        register(id, provider);
        return provider;
    }

    public static void registerNumber(ResourceLocation id, ToDoubleFunction<WidgetContext> provider) {
        Objects.requireNonNull(provider, "Number provider must not be null");
        register(id, context -> new JsonPrimitive(provider.applyAsDouble(context)));
    }

    public static void registerString(ResourceLocation id, Function<WidgetContext, String> provider) {
        Objects.requireNonNull(provider, "String provider must not be null");
        register(id, context -> {
            String value = provider.apply(context);
            return value == null ? JsonNull.INSTANCE : new JsonPrimitive(value);
        });
    }

    public static void registerBoolean(ResourceLocation id, Predicate<WidgetContext> provider) {
        Objects.requireNonNull(provider, "Boolean provider must not be null");
        register(id, context -> new JsonPrimitive(provider.test(context)));
    }

    public static void registerObject(ResourceLocation id, Function<WidgetContext, JsonObject> provider) {
        Objects.requireNonNull(provider, "Object provider must not be null");
        register(id, context -> {
            JsonObject value = provider.apply(context);
            return value == null ? JsonNull.INSTANCE : value;
        });
    }

    public static void registerList(ResourceLocation id, Function<WidgetContext, JsonArray> provider) {
        Objects.requireNonNull(provider, "List provider must not be null");
        register(id, context -> {
            JsonArray value = provider.apply(context);
            return value == null ? JsonNull.INSTANCE : value;
        });
    }

    private static JsonElement copy(JsonElement value) {
        return value == null ? JsonNull.INSTANCE : value.deepCopy();
    }
}
