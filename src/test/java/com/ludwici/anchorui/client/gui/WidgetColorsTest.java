package com.ludwici.anchorui.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WidgetColorsTest {
    @Test
    public void supportsOpaqueAndTransparentHexColors() {
        assertEquals(0xFFE6D6C3, WidgetColors.parse("#E6D6C3"));
        assertEquals(0x80403020, WidgetColors.parse("#80403020"));
        assertEquals(0x00000000, WidgetColors.parse("#00000000"));
    }

    @Test
    public void rejectsInvalidHexColor() {
        assertThrows(IllegalArgumentException.class, () -> WidgetColors.parse("#FFF"));
        assertThrows(IllegalArgumentException.class, () -> WidgetColors.parse("not-a-color"));
    }
}
