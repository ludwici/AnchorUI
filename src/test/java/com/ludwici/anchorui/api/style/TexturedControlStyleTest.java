package com.ludwici.anchorui.api.style;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TexturedControlStyleTest {
    @Test
    public void widgetTextureOverridesMergeWithSharedStyleWithoutLosingOtherVariants() {
        JsonObject style = JsonParser.parseString("""
                {
                  "anchorui:checkbox": {
                    "fillColor": "#FF112233",
                    "indicatorSize": 18,
                    "textures": {
                      "background": {
                        "normal": {
                          "texture": "mymod:textures/gui/frame.png",
                          "textureWidth": 48,
                          "textureHeight": 48
                        }
                      },
                      "indicator": {
                        "off": "mymod:textures/gui/off.png",
                        "on": "mymod:textures/gui/on.png",
                        "onHover": "mymod:textures/gui/on_hover.png"
                      }
                    }
                  }
                }
                """).getAsJsonObject();
        JsonObject overrides = JsonParser.parseString("""
                {
                  "textures": {
                    "indicator": {
                      "on": "mymod:textures/gui/overridden_on.png"
                    }
                  }
                }
                """).getAsJsonObject();
        LoreStyle.Element resolved = new LoreStyle(style).get(
                ResourceLocation.fromNamespaceAndPath("anchorui", "checkbox"), overrides);
        JsonObject textures = resolved.resolvedSettings.getAsJsonObject("textures");
        assertEquals("mymod:textures/gui/overridden_on.png",
                textures.getAsJsonObject("indicator").get("on").getAsString());
        assertEquals("mymod:textures/gui/off.png",
                textures.getAsJsonObject("indicator").get("off").getAsString());
        assertEquals("mymod:textures/gui/on_hover.png",
                textures.getAsJsonObject("indicator").get("onHover").getAsString());
        assertEquals(48,
                textures.getAsJsonObject("background").getAsJsonObject("normal").get("textureWidth").getAsInt());
        assertEquals(18, resolved.resolvedSettings.get("indicatorSize").getAsInt());
        assertTrue(resolved.resolvedSettings.has("fillColor"));
        assertEquals("mymod:textures/gui/on.png",
                style.getAsJsonObject("anchorui:checkbox").getAsJsonObject("textures")
                        .getAsJsonObject("indicator").get("on").getAsString());
    }
}
