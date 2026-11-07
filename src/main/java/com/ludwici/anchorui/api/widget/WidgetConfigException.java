package com.ludwici.anchorui.api.widget;

import net.minecraft.resources.ResourceLocation;

public final class WidgetConfigException extends IllegalArgumentException {
    public WidgetConfigException(ResourceLocation type, String id, String message, Throwable cause) {
        super("Invalid widget configuration for type '" + type + "', id '" + id + "': " + message, cause);
    }
}
