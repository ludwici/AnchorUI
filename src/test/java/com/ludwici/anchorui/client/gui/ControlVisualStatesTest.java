package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ControlVisualStatesTest {
    private static StateControlTextures textures(String json) {
        JsonObject settings = JsonParser.parseString(json).getAsJsonObject();
        return new StateControlTextures(null, settings);
    }

    @Test
    public void resolvesDistinctDisabledHoverAndKeyboardFocusSkinPriority() {
        assertArrayEquals(new String[]{"normal"}, ControlVisualStates.background(true, false, false));
        assertArrayEquals(new String[]{"hover", "normal"}, ControlVisualStates.background(true, true, false));
        assertArrayEquals(new String[]{"focused", "hover", "normal"}, ControlVisualStates.background(true, false, true));
        assertArrayEquals(new String[]{"hoverFocused", "focused", "hover", "normal"},
                ControlVisualStates.background(true, true, true));
        assertArrayEquals(new String[]{"disabled", "normal"}, ControlVisualStates.background(false, true, true));
    }

    @Test
    public void fallbackOutlineOnlyAppliesWithoutAnAppropriateFocusTexture() {
        StateControlTextures basic = textures("""
                {"textures":{"background":{"normal":"minecraft:textures/block/stone.png"}}}
                """);
        assertTrue(ControlVisualStates.needsFocusOutline(basic, true, false, true));
        assertFalse(ControlVisualStates.needsFocusOutline(basic, true, false, false));
        assertFalse(ControlVisualStates.needsFocusOutline(basic, false, false, true));

        StateControlTextures skinned = textures("""
                {"textures":{"background":{
                  "focused":"minecraft:textures/block/stone.png",
                  "hoverFocused":"minecraft:textures/block/stone.png"
                }}}
                """);
        assertFalse(ControlVisualStates.needsFocusOutline(skinned, true, false, true));
        assertFalse(ControlVisualStates.needsFocusOutline(skinned, true, true, true));
    }
}
