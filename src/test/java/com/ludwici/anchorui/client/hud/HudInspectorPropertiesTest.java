package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HudInspectorPropertiesTest {
    @Test
    public void flattensAllNestedCustomPropertiesAndArrays() {
        var json = JsonParser.parseString("""
                {
                  "type": "custom:bar",
                  "id": "custom:health",
                  "position": {"point": "TOPLEFT", "anchor": "BOTTOMLEFT", "x": 0, "y": 0},
                  "animation": {"enabled": true, "duration": 200},
                  "layers": [{"name": "base"}, {"name": "fill"}],
                  "tooltip": "One\\nTwo"
                }
                """).getAsJsonObject();
        List<String> lines = HudInspectorProperties.flatten(json);
        assertEquals("type: custom:bar", lines.getFirst());
        assertTrue(lines.contains("position.x: 0"));
        assertTrue(lines.contains("position.y: 0"));
        assertTrue(lines.contains("animation.enabled: true"));
        assertTrue(lines.contains("animation.duration: 200"));
        assertTrue(lines.contains("layers[0].name: base"));
        assertTrue(lines.contains("layers[1].name: fill"));
        assertTrue(lines.contains("tooltip: One\\nTwo"));
    }

    @Test
    public void displaysEmptyAndNullValues() {
        var json = JsonParser.parseString("""
                {"settings":{},"names":[],"other":null}
                """).getAsJsonObject();
        assertEquals(List.of("settings: {}", "names: []", "other: null"), HudInspectorProperties.flatten(json));
    }
}
