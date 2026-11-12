package com.ludwici.anchorui.api.state;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiControlsResourceTest {
    @Test
    public void demoControlsUseDeclaredStateAndValidOptions() throws IOException {
        String path = "assets/anchorui/anchorui/screens/ui_controls_demo.json";
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing demo screen " + path);
        JsonObject screen;
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            screen = JsonParser.parseReader(reader).getAsJsonObject();
        }
        UiLocalState state = new UiLocalState(screen.getAsJsonObject("state"));
        assertEquals("skills", state.get("tab").getAsString());
        assertTrue(state.get("showDetails").getAsBoolean());

        Set<String> types = Set.of("anchorui:checkbox", "anchorui:switch", "anchorui:select", "anchorui:tabs");
        Map<String, String> keys = Map.of(
                "anchorui:checkbox", "showDetails",
                "anchorui:switch", "notifications",
                "anchorui:select", "difficulty",
                "anchorui:tabs", "tab"
        );
        int count = 0;
        for (var entry : screen.getAsJsonArray("entities")) {
            JsonObject widget = entry.getAsJsonObject();
            String type = widget.get("type").getAsString();
            if (!types.contains(type)) {
                continue;
            }
            count++;
            String key = widget.get("stateKey").getAsString();
            assertEquals(keys.get(type), key);
            assertEquals("anchorui_test:event_notice", widget.get("onChange").getAsString());

            if (type.equals("anchorui:select") || type.equals("anchorui:tabs")) {
                var options = UiControlOptions.parse(widget, state, key);
                assertTrue(options.size() > 1);
                assertTrue(UiControlOptions.selectedIndex(options, state.get(key)) >= 0);
            } else {
                assertTrue(state.get(key).getAsJsonPrimitive().isBoolean());
            }
        }
        assertEquals(4, count);
    }
}
