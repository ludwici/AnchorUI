package com.ludwici.anchorui.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Validates identifiers used by public Java registries and JSON references.
 * External API IDs must include their namespace to avoid accidental collisions.
 */
public final class UiIdentifiers {
    private UiIdentifiers() {
    }

    public static ResourceLocation requireNamespaced(String value, String description) {
        if (value == null || value.isBlank() || value.indexOf(':') <= 0 || value.endsWith(":")) {
            throw new IllegalArgumentException(description + " must be a namespaced identifier (namespace:path): " + value);
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            throw new IllegalArgumentException("Invalid " + description + " identifier: " + value);
        }
        return id;
    }
}
