package com.ludwici.anchorui.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ColorPickerColorTest {
    @Test
    public void supportsSixDigitHexArgbAndRgbNumbers() {
        assertEquals(0xFF0A80FE, ColorPickerColor.parseHex("#0a80fe"));
        assertEquals(0x800A80FE, ColorPickerColor.parseHex("#800A80FE"));
        assertEquals(0xFF0A80FE, ColorPickerColor.parseRgb("10, 128, 254", 255));
        assertEquals(0x800A80FE, ColorPickerColor.parseRgb("rgb(10,128,254)", 128));
        assertEquals(0xFF0A80FE, ColorPickerColor.parse("rgb(10,128,254)"));
        assertEquals(0x800A80FE, ColorPickerColor.parse("#800A80FE"));
        assertEquals("#0A80FE", ColorPickerColor.hex(0xFF0A80FE));
        assertEquals("#800A80FE", ColorPickerColor.hex(0x800A80FE));
        assertEquals("10, 128, 254", ColorPickerColor.rgb(0x800A80FE));
    }

    @Test
    public void alphaInputPreservesRgbAndRoundTripsThroughArgbHex() {
        int base = ColorPickerColor.parseHex("#2698E3");
        assertEquals(0xFF2698E3, base);
        for (int alpha : new int[]{0, 1, 64, 128, 192, 254, 255}) {
            int result = ColorPickerColor.withAlpha(base, alpha);
            assertEquals(alpha, result >>> 24);
            assertEquals(base & 0xFFFFFF, result & 0xFFFFFF);
            assertEquals(result, ColorPickerColor.parse(ColorPickerColor.hex(result)));
            assertEquals(alpha, ColorPickerColor.parseAlpha(Integer.toString(alpha)));
        }
        assertEquals("#002698E3", ColorPickerColor.hex(ColorPickerColor.withAlpha(base, 0)));
        assertEquals("#802698E3", ColorPickerColor.hex(ColorPickerColor.withAlpha(base, 128)));
        assertEquals("#2698E3", ColorPickerColor.hex(ColorPickerColor.withAlpha(base, 255)));
        assertEquals(0x802698E3, ColorPickerColor.parseRgb("38, 152, 227", 128));
    }

    @Test
    public void alphaInputRejectsInvalidValues() {
        for (String invalid : new String[]{"-1", "256", "999", "0.5", "", "1a", "20%", "12 3"}) {
            assertThrows(IllegalArgumentException.class, () -> ColorPickerColor.parseAlpha(invalid), invalid);
        }
        assertThrows(IllegalArgumentException.class, () -> ColorPickerColor.withAlpha(0xFF2698E3, -1));
        assertThrows(IllegalArgumentException.class, () -> ColorPickerColor.withAlpha(0xFF2698E3, 256));
    }

    @Test
    public void refusesInvalidColorValues() {
        for (String value : new String[]{
                "#FFF", "#000000000", "#GG0022", "rgb(256,0,0)",
                "1, 2", "-1, 2, 3", "1, 2, 3, 4", "10.5, 1, 2",
                "not a color", "rgb(1, 2,)", ""
        }) {
            assertThrows(IllegalArgumentException.class, () -> ColorPickerColor.parse(value), value);
        }
    }

    @Test
    public void hsvConversionPreservesTransparencyAndRgbWithinRoundingTolerance() {
        int sample = 0x8040A3E7;
        float[] hsv = ColorPickerColor.hsv(sample);
        int result = ColorPickerColor.hsvColor(hsv[0], hsv[1], hsv[2], sample >>> 24);
        assertEquals(sample >>> 24, result >>> 24);
        for (int shift : new int[]{0, 8, 16}) {
            assertTrue(Math.abs((sample >>> shift & 255) - (result >>> shift & 255)) <= 1);
        }
    }
}
