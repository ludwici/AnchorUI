package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Reusable widget templates provided by Java mods for resource-pack template lists.
 *
 * <p>Register on the client before opening a UI that references the template.
 * JSON resource definitions with the same ID take precedence, allowing resource
 * packs to customize Java-provided defaults.</p>
 */
public final class WidgetTemplateRegistry {
    private static final Map<ResourceLocation, Supplier<JsonObject>> TEMPLATES = new ConcurrentHashMap<>();

    private WidgetTemplateRegistry() {
    }

    public static void register(ResourceLocation id, JsonObject definition) {
        Objects.requireNonNull(definition, "Widget template definition must not be null");
        JsonObject snapshot = definition.deepCopy();
        validate(id, snapshot);
        register(id, snapshot::deepCopy);
    }

    public static void register(ResourceLocation id, Supplier<JsonObject> factory) {
        Objects.requireNonNull(id, "Widget template ID must not be null");
        Objects.requireNonNull(factory, "Widget template factory must not be null");
        if (TEMPLATES.putIfAbsent(id, factory) != null) {
            throw new IllegalStateException("Anchor UI template already registered: " + id);
        }
    }

    /**
     * Returns fresh template copies. Factories are invoked when a template list
     * is expanded, not during rendering of individual frames.
     */
    public static Map<String, JsonObject> definitions() {
        Map<String, JsonObject> result = new LinkedHashMap<>();
        for (var entry : TEMPLATES.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonObject definition;
            try {
                definition = Objects.requireNonNull(entry.getValue().get(), "Template factory returned null");
                validate(id, definition);
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Cannot create Java widget template '" + id + "': " + exception.getMessage(), exception);
            }
            result.put(id.toString(), definition.deepCopy());
        }
        return result;
    }

    private static void validate(ResourceLocation id, JsonObject definition) {
        Objects.requireNonNull(id, "Widget template ID must not be null");
        if (definition == null || !(definition.has("type") || definition.has("ref"))) {
            throw new IllegalArgumentException("Widget template '" + id + "' requires 'type' or 'ref'");
        }
    }
}
