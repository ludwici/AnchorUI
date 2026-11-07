package com.ludwici.anchorui.client.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.PositionElement;
import com.ludwici.anchorui.api.style.ElementAnchor;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSceneLayoutTest {
    private JsonObject resource(int number) {
        String path = "assets/anchorui/lore/test/scenes/scene" + number + ".json";
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing bundled test scene: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private List<AnchorLayout.Frame> frames(int number) {
        List<AnchorLayout.Frame> result = new ArrayList<>();
        for (var json : resource(number).getAsJsonArray("entities")) {
            JsonObject element = json.getAsJsonObject();
            JsonObject size = element.getAsJsonObject("size");
            int width = size == null ? 0 : size.get("width").getAsInt();
            int height = size == null ? 0 : size.get("height").getAsInt();
            JsonObject position = element.getAsJsonObject("position");
            var point = ElementAnchor.fromString(position.get("point").getAsString());
            var anchor = ElementAnchor.fromString(position.get("anchor").getAsString());
            String relativeTo = position.get("relativeTo").getAsString();
            int x = position.get("x").getAsInt();
            int y = position.get("y").getAsInt();
            result.add(new AnchorLayout.Frame(element.get("id").getAsString(), new PositionElement(point, anchor, relativeTo, x, y), width, height));
        }
        return result;
    }

    @Test
    public void eachScenarioIsIncludedInResources() {
        for (int i = 1; i <= 8; i++) {
            assertTrue(resource(i).getAsJsonArray("entities").size() > 0);
        }
    }

    @Test
    public void sixVisualScenariosResolveAtMultipleSizes() {
        for (int i = 1; i <= 6; i++) {
            List<AnchorLayout.Frame> definitions = frames(i);
            assertEquals(definitions.size() + 1, AnchorLayout.resolve(320, 240, definitions).size());
            assertEquals(definitions.size() + 1, AnchorLayout.resolve(854, 480, definitions).size());
        }
    }

    @Test
    public void nineAnchorsCoverAllPositions() {
        var positions = AnchorLayout.resolve(400, 260, frames(1));
        assertEquals(8, positions.get("topleft").x());
        assertEquals(42, positions.get("top").y());
        assertEquals(400 - 95 - 8, positions.get("topright").x());
        assertEquals(400 / 2 - 95 / 2, positions.get("center").x());
        assertEquals(260 - 23 - 45, positions.get("bottom").y());
    }

    @Test
    public void hiddenFrameRemainsAvailableAsAnchor() {
        var positions = AnchorLayout.resolve(600, 300, frames(6));
        var hidden = positions.get("invisible");
        var child = positions.get("dependent");
        assertEquals(hidden.y() + hidden.height() + 8, child.y());
        assertEquals(hidden.x() + hidden.width() / 2, child.x() + child.width() / 2);
    }

    @Test
    public void unknownReferenceFallsBackToScreenWithWarning() {
        List<String> warnings = new ArrayList<>();
        var positions = AnchorLayout.resolve(400, 240, frames(7), warnings::add);
        assertEquals(new AnchorLayout.Bounds(200, 120, 160, 32), positions.get("broken"));
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("broken"));
        assertTrue(warnings.getFirst().contains("missing_target"));
    }

    @Test
    public void circularReferencesHaveExplicitError() {
        var exception = assertThrows(IllegalArgumentException.class, () -> AnchorLayout.resolve(400, 240, frames(8)));
        assertTrue(exception.getMessage().contains("Circular frame anchors"));
    }
}
