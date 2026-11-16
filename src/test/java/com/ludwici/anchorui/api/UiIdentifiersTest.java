package com.ludwici.anchorui.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiIdentifiersTest {
    @Test
    public void requiresExplicitNamespaceAndValidMinecraftIdentifier() {
        assertEquals("mymod:player.mana", UiIdentifiers.requireNamespaced("mymod:player.mana", "Data provider").toString());
        for (String value : new String[]{null, "", " ", "missing_namespace", ":thing", "mymod:", "mymod:UPPER", "mod:bad:name"}) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> UiIdentifiers.requireNamespaced(value, "Data provider"));
            assertTrue(exception.getMessage().contains("Data provider"));
        }
    }
}
