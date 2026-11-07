package com.ludwici.anchorui.api.widget;

import com.mojang.serialization.Codec;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class AnchorWidgetRegistry {
    private static final WidgetRegistryTable<ResourceLocation, WidgetFactory> FACTORIES = new WidgetRegistryTable<>();

    private AnchorWidgetRegistry() {
    }

    public static void register(ResourceLocation id, WidgetFactory factory) {
        FACTORIES.register(id, factory);
    }

    public static <T> void register(ResourceLocation id, Codec<T> codec, ConfiguredWidgetFactory<T> factory) {
        Objects.requireNonNull(codec, "Widget configuration codec must not be null");
        Objects.requireNonNull(factory, "Configured widget factory must not be null");
        register(id, (context, element, style) -> {
            T config;
            try {
                config = WidgetConfigDecoder.decode(codec, element.settings);
            } catch (IllegalArgumentException exception) {
                throw new WidgetConfigException(id, element.id, exception.getMessage(), exception);
            }
            return factory.create(context, element, style, config);
        });
    }

    public static Optional<WidgetFactory> find(ResourceLocation id) {
        return FACTORIES.find(id);
    }

    public static boolean contains(ResourceLocation id) {
        return FACTORIES.contains(id);
    }
}
