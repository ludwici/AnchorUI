package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.widget.WidgetContext;
import com.ludwici.anchorui.client.LoreLoader;
import net.minecraft.client.gui.GuiGraphics;

public final class SelectOptionAppearance {
    private final Integer background;
    private final Integer hover;
    private final Integer selected;
    private final Integer selectedHover;
    private final Integer textColor;
    private final Integer hoverTextColor;
    private final Integer selectedTextColor;
    private final StateControlTextures textures;

    private SelectOptionAppearance(WidgetContext context, JsonObject settings) {
        JsonObject variants = new JsonObject();
        if (settings.has("textures")) {
            JsonElement supplied = settings.get("textures");
            if (!supplied.isJsonObject()) {
                throw new IllegalArgumentException("Select option textures must be an object");
            }
            JsonObject raw = supplied.getAsJsonObject();
            if (raw.has("option")) {
                if (raw.size() != 1) {
                    throw new IllegalArgumentException("Select option textures may only contain the 'option' part");
                }
                JsonElement option = raw.get("option");
                if (option.isJsonObject() && !option.getAsJsonObject().has("texture")) {
                    variants = option.getAsJsonObject().deepCopy();
                } else {
                    variants.add("normal", option.deepCopy());
                }
            } else {
                variants = raw.deepCopy();
            }
        }

        background = readBackground(settings, "background", "normal", variants);
        hover = readBackground(settings, "hover", "hover", variants);
        selected = readBackground(settings, "selected", "selected", variants);
        selectedHover = readBackground(settings, "selectedHover", "selectedHover", variants);
        textColor = readColor(settings, "textColor");
        hoverTextColor = readColor(settings, "hoverTextColor");
        selectedTextColor = readColor(settings, "selectedTextColor");

        if (variants.isEmpty()) {
            textures = null;
        } else {
            JsonObject parts = new JsonObject();
            parts.add("option", variants);
            JsonObject visualSettings = new JsonObject();
            visualSettings.add("textures", parts);
            textures = new StateControlTextures(context, visualSettings);
        }
    }

    public static SelectOptionAppearance resolve(WidgetContext context, JsonElement choiceStyle) {
        if (choiceStyle == null || choiceStyle.isJsonNull()) {
            return null;
        }
        JsonObject settings;
        if (choiceStyle.isJsonPrimitive() && choiceStyle.getAsJsonPrimitive().isString()) {
            settings = LoreLoader.INSTANCE.getOptionStyle(choiceStyle.getAsString(), context.entity().namespace);
        } else if (choiceStyle.isJsonObject()) {
            settings = choiceStyle.getAsJsonObject().deepCopy();
            if (settings.has("ref")) {
                JsonElement reference = settings.remove("ref");
                if (!reference.isJsonPrimitive() || !reference.getAsJsonPrimitive().isString()
                        || reference.getAsString().isBlank()) {
                    throw new IllegalArgumentException("Select option style ref must be a non-empty id");
                }
                JsonObject base = LoreLoader.INSTANCE.getOptionStyle(reference.getAsString(), context.entity().namespace);
                settings = merge(base, settings);
            }
            if (settings.has("anchorui:option")) {
                JsonElement option = settings.get("anchorui:option");
                if (!option.isJsonObject()) {
                    throw new IllegalArgumentException("Select option style 'anchorui:option' must be an object");
                }
                settings = option.getAsJsonObject().deepCopy();
            }
        } else {
            throw new IllegalArgumentException("Select option style must be an id or an object");
        }
        return new SelectOptionAppearance(context, settings);
    }

    private static JsonObject merge(JsonObject base, JsonObject overrides) {
        JsonObject result = base.deepCopy();
        for (var entry : overrides.entrySet()) {
            JsonElement previous = result.get(entry.getKey());
            JsonElement next = entry.getValue();
            result.add(entry.getKey(), previous != null && previous.isJsonObject() && next.isJsonObject()
                    ? merge(previous.getAsJsonObject(), next.getAsJsonObject()) : next.deepCopy());
        }
        return result;
    }

    private static Integer readBackground(JsonObject settings, String key, String variant, JsonObject textures) {
        if (!settings.has(key)) {
            return null;
        }
        JsonElement value = settings.get(key);
        if (value.isJsonObject() && value.getAsJsonObject().has("texture")) {
            textures.add(variant, value.deepCopy());
            return null;
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                && value.getAsString().contains(":")) {
            textures.add(variant, value.deepCopy());
            return null;
        }
        if (value.isJsonObject() && value.getAsJsonObject().has("color")) {
            value = value.getAsJsonObject().get("color");
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Select option '" + key + "' must be a color or texture");
        }
        textures.remove(variant);
        return WidgetColors.parse(value.getAsString());
    }

    private static Integer readColor(JsonObject settings, String key) {
        if (!settings.has(key)) {
            return null;
        }
        JsonElement value = settings.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Select option '" + key + "' must be a color");
        }
        return WidgetColors.parse(value.getAsString());
    }

    public Integer backgroundColor(boolean isSelected, boolean isHovered) {
        if (isSelected && isHovered && selectedHover != null) {
            return selectedHover;
        }
        if (isSelected) {
            return selected;
        }
        if (isHovered) {
            return hover;
        }
        return background;
    }

    public int textColor(boolean isSelected, boolean isHovered, int fallback) {
        if (isSelected && selectedTextColor != null) {
            return selectedTextColor;
        }
        if (isHovered && hoverTextColor != null) {
            return hoverTextColor;
        }
        return textColor == null ? fallback : textColor;
    }

    public Integer backgroundColor(String state) {
        return switch (state) {
            case "normal" -> background;
            case "hover" -> hover;
            case "selected" -> selected;
            case "selectedHover" -> selectedHover;
            default -> null;
        };
    }

    public boolean hasBackgroundVariant(String state) {
        return backgroundColor(state) != null || textures != null && textures.hasVariant("option", state);
    }

    public boolean hasTextureVariant(String state) {
        return textures != null && textures.hasVariant("option", state);
    }

    public boolean renderBackgroundVariant(GuiGraphics graphics, int x, int y, int width, int height, String state) {
        Integer color = backgroundColor(state);
        if (color != null) {
            graphics.fill(x, y, x + width, y + height, color);
            return true;
        }
        return textures != null && textures.renderExact(graphics, "option", x, y, width, height, state);
    }
}
