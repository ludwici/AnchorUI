package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.GuiGraphics;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class StateControlTextures {
    private static final Map<String, Set<String>> PARTS = Map.of(
            "background", Set.of("normal", "hover", "focused", "hoverFocused", "disabled"),
            "indicator", Set.of("normal", "off", "on", "offHover", "onHover", "offDisabled", "onDisabled"),
            "track", Set.of("normal", "off", "on", "offHover", "onHover", "offDisabled", "onDisabled"),
            "thumb", Set.of("normal", "off", "on", "offHover", "onHover", "offDisabled", "onDisabled"),
            "arrow", Set.of("normal", "closed", "open", "disabled"),
            "popup", Set.of("normal"),
            "fill", Set.of("normal"),
            "frame", Set.of("normal"),
            "option", Set.of("normal", "hover", "selected", "selectedHover", "disabled"),
            "tab", Set.of("normal", "hover", "selected", "selectedHover", "disabled")
    );

    private final Map<String, Map<String, TextureRegion>> sprites;

    public StateControlTextures(WidgetContext context, JsonObject settings) {
        JsonElement configured = settings.get("textures");
        if (configured == null || configured.isJsonNull()) {
            sprites = Map.of();
            return;
        }
        if (!configured.isJsonObject()) {
            throw new IllegalArgumentException("Control 'textures' must be an object");
        }
        Map<String, Map<String, TextureRegion>> loaded = new LinkedHashMap<>();
        for (var entry : configured.getAsJsonObject().entrySet()) {
            String part = entry.getKey();
            Set<String> allowed = PARTS.get(part);
            if (allowed == null) {
                throw new IllegalArgumentException("Unknown control texture part: " + part);
            }
            JsonElement value = entry.getValue();
            Map<String, TextureRegion> variants = new LinkedHashMap<>();
            if (value.isJsonPrimitive() || value.isJsonObject() && value.getAsJsonObject().has("texture")) {
                variants.put("normal", read(context, value, part));
            } else if (value.isJsonObject()) {
                for (var state : value.getAsJsonObject().entrySet()) {
                    if (!allowed.contains(state.getKey())) {
                        throw new IllegalArgumentException("Unknown texture variant '" + state.getKey() + "' for '" + part + "'");
                    }
                    variants.put(state.getKey(), read(context, state.getValue(), part + "." + state.getKey()));
                }
            } else {
                throw new IllegalArgumentException("Control texture part '" + part + "' must be a texture or a variants object");
            }
            if (variants.isEmpty()) {
                throw new IllegalArgumentException("Control texture part '" + part + "' has no variants");
            }
            loaded.put(part, Map.copyOf(variants));
        }
        sprites = Map.copyOf(loaded);
    }

    private static TextureRegion read(WidgetContext context, JsonElement value, String label) {
        JsonObject settings;
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            settings = new JsonObject();
            settings.add("texture", value.deepCopy());
        } else if (value.isJsonObject() && value.getAsJsonObject().has("texture")) {
            settings = value.getAsJsonObject();
        } else {
            throw new IllegalArgumentException("Control texture '" + label + "' requires a texture path");
        }
        try {
            return new TextureRegion(context, settings);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid control texture '" + label + "': " + exception.getMessage(), exception);
        }
    }

    public boolean render(GuiGraphics graphics, String part, int x, int y, int width, int height, String... states) {
        Map<String, TextureRegion> variants = sprites.get(part);
        if (variants == null || width <= 0 || height <= 0) {
            return false;
        }
        TextureRegion texture = null;
        for (String state : states) {
            texture = variants.get(state);
            if (texture != null) {
                break;
            }
        }
        if (texture == null) {
            texture = variants.get("normal");
        }
        if (texture == null) {
            return false;
        }
        texture.render(graphics, x, y, width, height);
        return true;
    }

    public boolean renderClippedWidth(GuiGraphics graphics, String part, int x, int y, int width, int height, int visibleWidth, String... states) {
        Map<String, TextureRegion> variants = sprites.get(part);
        if (variants == null || width <= 0 || height <= 0) {
            return false;
        }
        TextureRegion texture = null;
        for (String state : states) {
            texture = variants.get(state);
            if (texture != null) {
                break;
            }
        }
        if (texture == null) {
            texture = variants.get("normal");
        }
        if (texture == null) {
            return false;
        }
        texture.renderClippedWidth(graphics, x, y, width, height, visibleWidth);
        return true;
    }

    public boolean has(String part) {
        return sprites.containsKey(part);
    }

    public boolean hasVariant(String part, String state) {
        Map<String, TextureRegion> variants = sprites.get(part);
        return variants != null && variants.containsKey(state);
    }

    public boolean renderExact(GuiGraphics graphics, String part, int x, int y, int width, int height, String state) {
        Map<String, TextureRegion> variants = sprites.get(part);
        if (variants == null || width <= 0 || height <= 0) {
            return false;
        }
        TextureRegion texture = variants.get(state);
        if (texture == null) {
            return false;
        }
        texture.render(graphics, x, y, width, height);
        return true;
    }
}
