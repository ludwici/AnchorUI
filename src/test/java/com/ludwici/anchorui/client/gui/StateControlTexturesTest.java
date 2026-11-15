package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StateControlTexturesTest {
    private static JsonObject object(String raw) {
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    @Test
    public void acceptsTexturePathsAtlasesTintsAndNineSlice() {
        StateControlTextures textures = new StateControlTextures(null, object("""
                {
                  "textures": {
                    "background": {
                      "normal": {
                        "texture": "anchorui:textures/gui/nine_slice_panel.png",
                        "textureWidth": 48,
                        "textureHeight": 48,
                        "u": 0,
                        "v": 0,
                        "regionWidth": 48,
                        "regionHeight": 48,
                        "nineSlice": {
                          "left": 12,
                          "top": 12,
                          "right": 12,
                          "bottom": 12
                        },
                        "tint": "#FFAA7744"
                      },
                      "hover": "minecraft:textures/block/stone.png",
                      "disabled": "minecraft:textures/block/stone.png"
                    },
                    "indicator": {
                      "off": "minecraft:textures/block/stone.png",
                      "on": "minecraft:textures/block/stone.png",
                      "onHover": "minecraft:textures/block/stone.png",
                      "onDisabled": "minecraft:textures/block/stone.png"
                    },
                    "track": "minecraft:textures/block/stone.png",
                    "thumb": "minecraft:textures/block/stone.png",
                    "arrow": {
                      "open": "minecraft:textures/block/stone.png",
                      "closed": "minecraft:textures/block/stone.png"
                    },
                    "popup": "minecraft:textures/block/stone.png",
                    "fill": "minecraft:textures/block/stone.png",
                    "frame": "minecraft:textures/block/stone.png",
                    "option": {
                      "normal": "minecraft:textures/block/stone.png",
                      "selected": "minecraft:textures/block/stone.png"
                    },
                    "tab": {
                      "normal": "minecraft:textures/block/stone.png",
                      "hover": "minecraft:textures/block/stone.png",
                      "selected": "minecraft:textures/block/stone.png"
                    }
                  }
                }
                """));
        for (String part : new String[]{"background", "indicator", "track", "thumb", "arrow", "popup", "option", "tab", "fill", "frame"}) {
            assertTrue(textures.has(part), part);
        }
        assertTrue(textures.hasVariant("option", "selected"));
        assertFalse(textures.hasVariant("option", "selectedHover"));
        assertTrue(textures.hasVariant("fill", "normal"));
        assertFalse(textures.hasVariant("fill", "hover"));
        assertFalse(textures.has("unknown"));
        assertFalse(new StateControlTextures(null, new JsonObject()).has("background"));
    }

    @Test
    public void detectsMissingTexturesUnknownPartsAndInvalidVariants() {
        for (String raw : new String[]{
                "{\"textures\":[]}",
                "{\"textures\":{\"unsupported\":\"minecraft:textures/block/stone.png\"}}",
                "{\"textures\":{\"background\":{}}}",
                "{\"textures\":{\"background\":{\"wrong\":\"minecraft:textures/block/stone.png\"}}}",
                "{\"textures\":{\"tab\":{\"checked\":\"minecraft:textures/block/stone.png\"}}}",
                "{\"textures\":{\"arrow\":{\"open\":{}}}}",
                "{\"textures\":{\"background\":{\"texture\":\"\"}}}",
                "{\"textures\":{\"background\":{\"texture\":\"minecraft:textures/block/stone.png\",\"textureWidth\":16,\"textureHeight\":16,\"u\":15,\"regionWidth\":5}}}",
                "{\"textures\":{\"background\":{\"texture\":\"minecraft:textures/block/stone.png\",\"textureWidth\":16,\"textureHeight\":16,\"regionWidth\":16,\"regionHeight\":16,\"nineSlice\":{\"left\":8,\"right\":8}}}}"
        }) {
            assertThrows(IllegalArgumentException.class, () -> new StateControlTextures(null, object(raw)), raw);
        }
    }

    @Test
    public void bundledSelectOptionStyleIsAvailable() throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream(
                "assets/anchorui/anchorui/style/rare_option.json");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject style = JsonParser.parseReader(reader).getAsJsonObject();
            assertTrue(style.getAsJsonObject("anchorui:option").has("selected"));
        }
    }

    @Test
    public void bundledResourceSkinDefinesVariantsForEachControl() throws IOException {
        var stream = getClass().getClassLoader().getResourceAsStream(
                "assets/anchorui/anchorui/style/textured_controls.json");
        assertNotNull(stream);
        JsonObject style;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            style = JsonParser.parseReader(reader).getAsJsonObject();
        }
        for (String type : new String[]{"checkbox", "switch", "select", "tabs"}) {
            JsonObject config = style.getAsJsonObject("anchorui:" + type);
            assertNotNull(config);
            assertTrue(new StateControlTextures(null, config).has("background"));
        }

        var demo = getClass().getClassLoader().getResourceAsStream(
                "assets/anchorui/anchorui/screens/ui_controls_textured_demo.json");
        assertNotNull(demo);
        try (var reader = new InputStreamReader(demo, StandardCharsets.UTF_8)) {
            assertTrue(JsonParser.parseReader(reader).getAsJsonObject().get("style")
                    .getAsString().equals("anchorui:style/textured_controls"));
        }
    }
}
