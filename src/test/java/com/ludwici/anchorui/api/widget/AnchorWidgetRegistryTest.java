package com.ludwici.anchorui.api.widget;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnchorWidgetRegistryTest {
    @Test
    public void registrationLookupAndDuplicates() {
        ResourceLocation type = ResourceLocation.fromNamespaceAndPath("anchorui_test", "registry_unit");
        WidgetFactory factory = (context, element, style) -> null;

        assertFalse(AnchorWidgetRegistry.contains(type));
        AnchorWidgetRegistry.register(type, factory);
        assertTrue(AnchorWidgetRegistry.contains(type));
        assertSame(factory, AnchorWidgetRegistry.find(type).orElseThrow());
        assertThrows(IllegalStateException.class, () -> AnchorWidgetRegistry.register(type, factory));
        assertTrue(AnchorWidgetRegistry.find(ResourceLocation.fromNamespaceAndPath("anchorui_test", "not_registered")).isEmpty());
    }
}
