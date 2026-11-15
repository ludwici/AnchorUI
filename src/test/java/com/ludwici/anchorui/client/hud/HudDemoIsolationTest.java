package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HudDemoIsolationTest {
    private JsonObject hud(String path) {
        String resource = "assets/" + path;
        var stream = getClass().getClassLoader().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("Missing demo resource " + resource);
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    public void rpgAndAnchorDemosUseSeparateTestGroups() {
        JsonObject anchor = hud("anchorui/anchorui/hud/hud1.json");
        JsonObject extra = hud("anchorui_test/anchorui/hud/hud1_extra.json");
        JsonObject rpg = hud("anchorui/anchorui/hud/rpg_status.json");
        assertTrue(anchor.get("testOnly").getAsBoolean());
        assertTrue(extra.get("testOnly").getAsBoolean());
        assertTrue(rpg.get("testOnly").getAsBoolean());
        assertEquals("hud1", anchor.get("testGroup").getAsString());
        assertEquals("hud1", extra.get("testGroup").getAsString());
        assertEquals("rpg", rpg.get("testGroup").getAsString());
        assertNotEquals(anchor.get("testGroup").getAsString(), rpg.get("testGroup").getAsString());
    }

    @Test
    public void bothRpgBarsHaveIdenticalOutlinePaddingAndDimensions() {
        JsonObject rpg = hud("anchorui/anchorui/hud/rpg_status.json");
        JsonObject health = rpg.getAsJsonArray("entities").get(1).getAsJsonObject();
        JsonObject food = rpg.getAsJsonArray("entities").get(2).getAsJsonObject();
        JsonObject rawStyle = hud("anchorui/anchorui/style/rpg_progress.json");
        com.ludwici.anchorui.api.style.LoreStyle style = new com.ludwici.anchorui.api.style.LoreStyle(rawStyle);
        net.minecraft.resources.ResourceLocation type = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anchorui", "progress");
        var healthStyle = style.get(type, health);
        var foodStyle = style.get(type, food);
        assertEquals(health.getAsJsonObject("size"), food.getAsJsonObject("size"));
        assertEquals(healthStyle.padding.left, foodStyle.padding.left);
        assertEquals(healthStyle.padding.top, foodStyle.padding.top);
        assertEquals(1, foodStyle.padding.left);
        assertEquals(1, foodStyle.padding.top);
        assertEquals(healthStyle.background.color, foodStyle.background.color);
        assertEquals(healthStyle.resolvedSettings.get("borderWidth"), foodStyle.resolvedSettings.get("borderWidth"));
        assertEquals(healthStyle.resolvedSettings.get("borderColor"), foodStyle.resolvedSettings.get("borderColor"));
        assertNotEquals(healthStyle.resolvedSettings.get("fillColor"), foodStyle.resolvedSettings.get("fillColor"));
    }

    @Test
    public void externalStarterUsesMatchingBarStyles() {
        var resource = java.nio.file.Path.of("examples/resourcepack/AnchorUI-Starter/assets/mypack/anchorui/hud/status.json");
        try {
            JsonObject pack = JsonParser.parseString(java.nio.file.Files.readString(resource, StandardCharsets.UTF_8)).getAsJsonObject();
            var health = pack.getAsJsonArray("entities").get(1).getAsJsonObject();
            var food = pack.getAsJsonArray("entities").get(2).getAsJsonObject();
            assertEquals(health.getAsJsonObject("size"), food.getAsJsonObject("size"));
            assertEquals(health.getAsJsonObject("padding"), food.getAsJsonObject("padding"));
            assertEquals(health.getAsJsonObject("background"), food.getAsJsonObject("background"));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
