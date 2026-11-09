package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumberBindingSpecTest {
    @Test
    public void acceptsStaticNumbersAndMissingOptionalValues() {
        NumberBindingSpec staticValue = NumberBindingSpec.parse(JsonParser.parseString("12.5"), 0);
        assertNull(staticValue.source());
        assertEquals(12.5, staticValue.literal());
        assertEquals(12.5, staticValue.fallback());

        NumberBindingSpec missing = NumberBindingSpec.parse(null, 100);
        assertNull(missing.source());
        assertEquals(100, missing.literal());
    }

    @Test
    public void acceptsDataSourcesWithExplicitFallbacks() {
        NumberBindingSpec binding = NumberBindingSpec.parse(JsonParser.parseString("""
                {"bind":"anchorui:player.health","fallback":1}
                """), 0);
        assertEquals("anchorui:player.health", binding.source());
        assertEquals(1.0, binding.fallback());
    }

    @Test
    public void rejectsUnsupportedAndInvalidData() {
        assertThrows(IllegalArgumentException.class, () -> NumberBindingSpec.parse(JsonParser.parseString("\"12\""), 0));
        assertThrows(IllegalArgumentException.class, () -> NumberBindingSpec.parse(JsonParser.parseString("true"), 0));
        assertThrows(IllegalArgumentException.class, () -> NumberBindingSpec.parse(JsonParser.parseString("{}"), 0));
        assertThrows(IllegalArgumentException.class, () -> NumberBindingSpec.parse(JsonParser.parseString("{\"bind\":\"\"}"), 0));
        assertThrows(IllegalArgumentException.class, () -> NumberBindingSpec.parse(JsonParser.parseString("{\"bind\":\"example:value\",\"fallback\":\"bad\"}"), 0));
    }

    @Test
    public void bundledRpgHudUsesLiveHealthAndFoodBindings() {
        String path = "assets/anchorui/anchorui/hud/rpg_status.json";
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertTrue(stream != null, "Missing RPG HUD fixture");
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject doc = JsonParser.parseReader(reader).getAsJsonObject();
                assertTrue(doc.get("testOnly").getAsBoolean());
                var entities = doc.getAsJsonArray("entities");
                assertEquals("anchorui:progress", entities.get(1).getAsJsonObject().get("type").getAsString());
                assertEquals("anchorui:player.health", entities.get(1).getAsJsonObject().getAsJsonObject("value").get("bind").getAsString());
                assertEquals("anchorui:player.max_health", entities.get(1).getAsJsonObject().getAsJsonObject("max").get("bind").getAsString());
                assertEquals("anchorui:player.food", entities.get(2).getAsJsonObject().getAsJsonObject("value").get("bind").getAsString());
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
