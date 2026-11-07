package com.ludwici.anchorui.api.widget;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class AnchorWidgetRegistry {
    private static final WidgetRegistryTable<ResourceLocation, WidgetFactory> FACTORIES = new WidgetRegistryTable<>();

    private AnchorWidgetRegistry() {
    }

    public static void register(ResourceLocation id, WidgetFactory factory) {
        FACTORIES.register(id, factory);
    }

    public static Optional<WidgetFactory> find(ResourceLocation id) {
        return FACTORIES.find(id);
    }

    public static boolean contains(ResourceLocation id) {
        return FACTORIES.contains(id);
    }
}
