package com.ludwici.anchorui.api.condition;

import com.google.gson.JsonElement;
import com.ludwici.anchorui.api.UiIdentifiers;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Operations contributed by other mods for use in Condition JSON expressions.
 *
 * <p>Register during mod initialization, before resource-pack HUDs/screens are loaded.
 * Operations are client-side checks, not authorization for gameplay actions.</p>
 */
public final class ConditionOperationRegistry {
    public record Definition(boolean unary, BiPredicate<JsonElement, JsonElement> predicate) {
        public Definition {
            Objects.requireNonNull(predicate, "Condition operation predicate must not be null");
        }

        public boolean test(JsonElement left, JsonElement right) {
            return predicate.test(left, right);
        }
    }

    private static final Map<String, Definition> OPERATIONS = new ConcurrentHashMap<>();

    private ConditionOperationRegistry() {
    }

    /**
     * Register a binary operation, used with left, op and right in JSON.
     */
    public static void register(ResourceLocation id, BiPredicate<JsonElement, JsonElement> predicate) {
        Objects.requireNonNull(id, "Condition operation id must not be null");
        register(id.toString(), predicate);
    }

    /**
     * Register a binary operation with a namespaced string ID.
     */
    public static void register(String id, BiPredicate<JsonElement, JsonElement> predicate) {
        registerDefinition(id, new Definition(false, predicate));
    }

    /**
     * Register a unary operation, used with left and op (without right) in JSON.
     */
    public static void registerUnary(ResourceLocation id, Predicate<JsonElement> predicate) {
        Objects.requireNonNull(id, "Condition operation id must not be null");
        registerUnary(id.toString(), predicate);
    }

    /**
     * Register a unary operation with a namespaced string ID.
     */
    public static void registerUnary(String id, Predicate<JsonElement> predicate) {
        Objects.requireNonNull(predicate, "Condition operation predicate must not be null");
        registerDefinition(id, new Definition(true, (left, ignored) -> predicate.test(left)));
    }

    private static void registerDefinition(String id, Definition definition) {
        Objects.requireNonNull(id, "Condition operation id must not be null");
        String qualifiedId = UiIdentifiers.requireNamespaced(id, "Condition operation").toString();
        if (OPERATIONS.putIfAbsent(qualifiedId, definition) != null) {
            throw new IllegalStateException("Anchor UI condition operation already registered: " + id);
        }
    }

    public static Optional<Definition> find(String id) {
        return Optional.ofNullable(OPERATIONS.get(id));
    }
}
