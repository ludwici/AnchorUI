package com.ludwici.anchorui.api.style;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SharedVisualStyleTest {
    @Test
    public void resolvesProgressAndPanelAppearanceFromReusableStyle() {
        JsonObject style = JsonParser.parseString("""
                {
                  "anchorui:progress": {
                    "fillColor": "#FF335577",
                    "borderColor": "#FF99AABB",
                    "borderWidth": 2,
                    "textures": {
                      "frame": "minecraft:textures/block/stone.png"
                    }
                  },
                  "anchorui:panel": {
                    "fillColor": "#FF101820",
                    "radius": 8,
                    "texture": "minecraft:textures/block/stone.png"
                  }
                }
                """).getAsJsonObject();
        LoreStyle shared = new LoreStyle(style);
        LoreStyle.Element progress = shared.get(ResourceLocation.fromNamespaceAndPath("anchorui", "progress"), JsonParser.parseString("{\"fillColor\":\"#FFCC4455\"}").getAsJsonObject());
        assertEquals("#FFCC4455", progress.resolvedSettings.get("fillColor").getAsString());
        assertEquals("#FF99AABB", progress.resolvedSettings.get("borderColor").getAsString());
        assertEquals(2, progress.resolvedSettings.get("borderWidth").getAsInt());
        assertNotNull(progress.resolvedSettings.getAsJsonObject("textures").get("frame"));

        LoreStyle.Element panel = shared.get(ResourceLocation.fromNamespaceAndPath("anchorui", "panel"), new JsonObject());
        assertEquals(8, panel.resolvedSettings.get("radius").getAsInt());
        assertEquals("#FF101820", panel.resolvedSettings.get("fillColor").getAsString());
        assertEquals("minecraft:textures/block/stone.png", panel.resolvedSettings.get("texture").getAsString());
    }
}
