package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SelectOptionAppearanceTest {
    @Test
    public void stateColorsAndTextOverridesRespectInheritedFallbacks() {
        SelectOptionAppearance appearance = SelectOptionAppearance.resolve(null, JsonParser.parseString("""
                {
                  "textColor": "#FFE2B8FF",
                  "background": "#FF101010",
                  "selected": "#FF61348A",
                  "selectedTextColor": "#FFFFFFFF"
                }
                """));
        assertEquals(Integer.valueOf(0xFF101010), appearance.backgroundColor(false, false));
        assertNull(appearance.backgroundColor(false, true));
        assertEquals(Integer.valueOf(0xFF61348A), appearance.backgroundColor(true, false));
        assertEquals(Integer.valueOf(0xFF61348A), appearance.backgroundColor(true, true));
        assertEquals(0xFFE2B8FF, appearance.textColor(false, false, 0xFFABCDEF));
        assertEquals(0xFFFFFFFF, appearance.textColor(true, true, 0xFFABCDEF));
    }

    @Test
    public void optionColorsOverrideAnInheritedSpriteForTheSameState() {
        SelectOptionAppearance appearance = SelectOptionAppearance.resolve(null, JsonParser.parseString("""
                {
                  "textures": {
                    "option": {
                      "selected": "minecraft:textures/block/stone.png",
                      "hover": "minecraft:textures/block/stone.png"
                    }
                  },
                  "selected": "#FF704996"
                }
                """));
        assertEquals(Integer.valueOf(0xFF704996), appearance.backgroundColor("selected"));
        org.junit.jupiter.api.Assertions.assertTrue(appearance.hasBackgroundVariant("selected"));
        org.junit.jupiter.api.Assertions.assertFalse(appearance.hasTextureVariant("selected"));
        org.junit.jupiter.api.Assertions.assertTrue(appearance.hasTextureVariant("hover"));
    }

    @Test
    public void rejectsMalformedInlineStyleFields() {
        for (String raw : new String[]{
                "{\"background\":[]}",
                "{\"hover\":false}",
                "{\"textColor\":7}",
                "{\"ref\":123}",
                "{\"textures\":[]}"
        }) {
            assertThrows(IllegalArgumentException.class, () ->
                    SelectOptionAppearance.resolve(null, JsonParser.parseString(raw)), raw);
        }
    }
}
