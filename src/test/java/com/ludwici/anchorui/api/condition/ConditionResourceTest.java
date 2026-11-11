package com.ludwici.anchorui.api.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.binding.DataValuePaths;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConditionResourceTest {
    private JsonObject resource(String path) throws IOException {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing Conditions demo: " + path);
            }
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
    }

    @Test
    public void demoConditionsCompileAndHaveRegisteredSourceNames() throws IOException {
        JsonObject screen = resource("assets/anchorui/anchorui/screens/conditions.json");
        int count = 0;
        for (JsonElement value : screen.getAsJsonArray("entities")) {
            JsonObject widget = value.getAsJsonObject();
            for (String property : new String[]{"visibleWhen", "enabledWhen"}) {
                if (widget.has(property)) {
                    var compiled = ConditionExpression.parse(widget.get(property), true,
                            !"enabledWhen".equals(property));
                    assertFalse(compiled.sources().isEmpty());
                    assertTrue(compiled.sources().stream().allMatch(id -> id.startsWith("anchorui:")));
                    count++;
                }
            }
        }
        assertEquals(4, count);

        JsonObject hud = resource("assets/anchorui/anchorui/hud/conditions_demo.json");
        assertEquals("conditions", hud.get("testGroup").getAsString());
        assertTrue(hud.get("testOnly").getAsBoolean());
        int hudConditions = 0;
        for (JsonElement widget : hud.getAsJsonArray("entities")) {
            if (widget.getAsJsonObject().has("visibleWhen")) {
                ConditionExpression.parse(widget.getAsJsonObject().get("visibleWhen"), true);
                hudConditions++;
            }
        }
        assertEquals(3, hudConditions);
    }

    @Test
    public void demoLevelButtonIsDisabledUntilThresholdIsMet() throws IOException {
        JsonObject screen = resource("assets/anchorui/anchorui/screens/conditions.json");
        var button = screen.getAsJsonArray("entities").get(3).getAsJsonObject();
        assertEquals("anchorui:condition_level_button", button.get("id").getAsString());
        ConditionExpression condition = ConditionExpression.parse(button.get("enabledWhen"), true, false);

        assertFalse(condition.evaluate(values(Map.of("anchorui:player.level", JsonParser.parseString("4")))));
        assertTrue(condition.evaluate(values(Map.of("anchorui:player.level", JsonParser.parseString("5")))));
        assertFalse(condition.evaluate(values(Map.of()));
    }

    private ConditionExpression.Resolver values(Map<String, JsonElement> values) {
        return (source, path) -> DataValuePaths.resolve(values.get(source), path);
    }
}
