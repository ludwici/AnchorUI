package com.ludwici.anchorui.api.style;

import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ColorValuesTest {
    @Test
    public void sixAndEightDigitValuesKeepRgbAndAlphaChannels() {
        assertEquals(0xFFFF8040, ColorValues.parse("#FF8040"));
        assertEquals(0x80112233, ColorValues.parse("#80112233"));
        assertEquals(0x00112233, ColorValues.parse("00112233"));
        assertEquals("#112233", ColorValues.format(0xFF112233));
        assertEquals("#00112233", ColorValues.format(0x00112233));
    }

    @Test
    public void styleTextColorRespectsAlphaAndOverridesInheritedColor() {
        var root = JsonParser.parseString("""
                {
                  "anchorui:text": {
                    "text": {
                      "color": "#8044CCEE"
                    }
                  }
                }
                """).getAsJsonObject();
        LoreStyle style = new LoreStyle(root);
        var base = style.get(ResourceLocation.parse("anchorui:text"), null);
        assertEquals(0x8044CCEE, base.textColor);
        var overrides = JsonParser.parseString("""
                {
                  "color": "#00112233"
                }
                """).getAsJsonObject();
        assertEquals(0x00112233,
                style.get(ResourceLocation.parse("anchorui:text"), overrides).textColor);
        assertEquals(0xFFFFE278, new LoreStyle(new com.google.gson.JsonObject())
                .get(ResourceLocation.parse("anchorui:text"), null).textColor);
    }

    @Test
    public void backgroundColorsFollowSameHexRulesAsTextColors() {
        var json = JsonParser.parseString("""
                {
                  "color": "#99123456"
                }
                """).getAsJsonObject();
        assertEquals(0x99123456, new BackgroundElement(json).color);
        assertEquals(0xFF123456, new BackgroundElement(
                JsonParser.parseString("""
                        {
                          "color": "#123456"
                        }
                        """).getAsJsonObject()).color);
    }

    @Test
    public void malformedColorsReportOriginalValueAndExpectedFormat() {
        for (String candidate : new String[]{"#FFF", "#FF00GG", "FF1234567", "", "#12345"}) {
            var exception = assertThrows(IllegalArgumentException.class,
                    () -> ColorValues.parse(candidate));
            assertTrue(exception.getMessage().contains(candidate));
            assertTrue(exception.getMessage().contains("#RRGGBB"));
        }
    }
}
