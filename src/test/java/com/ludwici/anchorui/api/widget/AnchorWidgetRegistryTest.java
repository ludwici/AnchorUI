package com.ludwici.anchorui.api.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnchorWidgetRegistryTest {
    @Test
    public void registrationLookupAndDuplicates() {
        WidgetRegistryTable<String, Object> registry = new WidgetRegistryTable<>();
        Object factory = new Object();

        assertFalse(registry.contains("anchorui_test:registry_unit"));
        registry.register("anchorui_test:registry_unit", factory);
        assertTrue(registry.contains("anchorui_test:registry_unit"));
        assertSame(factory, registry.find("anchorui_test:registry_unit").orElseThrow());
        assertThrows(IllegalStateException.class, () -> registry.register("anchorui_test:registry_unit", factory));
        assertTrue(registry.find("anchorui_test:not_registered").isEmpty());
        assertThrows(NullPointerException.class, () -> registry.register(null, factory));
        assertThrows(NullPointerException.class, () -> registry.register("anchorui_test:null_factory", null));
    }
}
