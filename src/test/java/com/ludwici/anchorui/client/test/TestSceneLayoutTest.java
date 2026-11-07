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
        for (int i = 1; i <= 14; i++) {
            assertTrue(resource(i).getAsJsonArray("entities").size() > 0);
        }
    }

    @Test
    public void visualScenariosResolveAtMultipleSizes() {
        for (int i = 1; i <= 6; i++) {
            List<AnchorLayout.Frame> definitions = frames(i);
            assertEquals(definitions.size() + 1, AnchorLayout.resolve(320, 240, definitions).size());
            assertEquals(definitions.size() + 1, AnchorLayout.resolve(854, 480, definitions).size());
        }
        List<AnchorLayout.Frame> textDefinitions = frames(9);
        assertEquals(textDefinitions.size() + 1, AnchorLayout.resolve(320, 240, textDefinitions).size());
        assertEquals(textDefinitions.size() + 1, AnchorLayout.resolve(854, 480, textDefinitions).size());
        List<AnchorLayout.Frame> richTextDefinitions = frames(10);
        assertEquals(richTextDefinitions.size() + 1, AnchorLayout.resolve(320, 240, richTextDefinitions).size());
        assertEquals(richTextDefinitions.size() + 1, AnchorLayout.resolve(854, 480, richTextDefinitions).size());
        List<AnchorLayout.Frame> customDefinitions = frames(11);
        assertEquals(customDefinitions.size() + 1, AnchorLayout.resolve(320, 240, customDefinitions).size());
        assertEquals(customDefinitions.size() + 1, AnchorLayout.resolve(854, 480, customDefinitions).size());
    }

    @Test
    public void customWidgetSceneUsesNamespacedTypesAndProperties() {
        var entries = resource(11).getAsJsonArray("entities");
        JsonObject counterA = entries.get(1).getAsJsonObject();
        JsonObject counterB = entries.get(2).getAsJsonObject();
        assertEquals("anchorui_test:counter", counterA.get("type").getAsString());
        assertEquals("anchorui_test:counter", counterB.get("type").getAsString());
        assertEquals(1, counterA.get("step").getAsInt());
        assertEquals(5, counterB.get("step").getAsInt());
        assertEquals("counter_a", counterB.getAsJsonObject("position").get("relativeTo").getAsString());
    }

    @Test
    public void unknownAndBrokenFactoriesAreIsolatedFromValidElements() {
        var entries = resource(12).getAsJsonArray("entities");
        assertEquals("unregistered:missing_widget", entries.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("anchorui_test:broken", entries.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals("anchorui:text", entries.get(2).getAsJsonObject().get("type").getAsString());
        assertEquals("anchorui:text", entries.get(3).getAsJsonObject().get("type").getAsString());
    }

    @Test
    public void bundledWidgetTypesUseAnchorUiNamespace() {
        for (int i = 1; i <= 10; i++) {
            for (var entry : resource(i).getAsJsonArray("entities")) {
                String type = entry.getAsJsonObject().get("type").getAsString();
                assertTrue(List.of("anchorui:text", "anchorui:model", "anchorui:list").contains(type), "Unexpected widget type in scene" + i + ": " + type);
            }
        }
    }

    @Test
    public void textSceneCoversAlignmentsAndEllipsis() {
        JsonObject scene = resource(9);
        var elements = scene.getAsJsonArray("entities");
        List<String> alignments = new ArrayList<>();
        for (var json : elements) {
            JsonObject element = json.getAsJsonObject();
            assertEquals("anchorui:text", element.get("type").getAsString());
            alignments.add(element.get("align").getAsString());
        }
        assertTrue(alignments.containsAll(List.of("left", "center", "right")));
        JsonObject ellipsis = elements.get(elements.size() - 1).getAsJsonObject();
        assertTrue(ellipsis.get("text").getAsString().length() > 70);
        assertTrue(ellipsis.getAsJsonObject("size").get("width").getAsInt() < 160);
    }

    @Test
    public void richTextSceneContainsTagsWrapAndHardLineBreaks() {
        var elements = resource(10).getAsJsonArray("entities");
        JsonObject formatted = elements.get(0).getAsJsonObject();
        JsonObject tooltip = elements.get(1).getAsJsonObject();
        JsonObject wrapped = elements.get(2).getAsJsonObject();
        JsonObject explicit = elements.get(3).getAsJsonObject();
        assertTrue(formatted.get("text").getAsString().contains("<b>"));
        assertTrue(formatted.get("text").getAsString().contains("<i>"));
        assertTrue(formatted.get("text").getAsString().contains("<u>"));
        assertTrue(tooltip.get("text").getAsString().contains("<t id="));
        assertTrue(wrapped.get("wrap").getAsBoolean());
        assertEquals(0, wrapped.getAsJsonObject("size").get("height").getAsInt());
        assertTrue(explicit.get("text").getAsString().contains("\n"));
    }

    @Test
    public void recordCodecSceneHasValidDefaultsAndInvalidCases() {
        var entries = resource(13).getAsJsonArray("entities");
        JsonObject defaultCounter = entries.get(1).getAsJsonObject();
        JsonObject configuredCounter = entries.get(2).getAsJsonObject();
        JsonObject wrongType = entries.get(3).getAsJsonObject();
        JsonObject wrongRange = entries.get(4).getAsJsonObject();
        assertEquals("anchorui_test:counter", defaultCounter.get("type").getAsString());
        assertTrue(!defaultCounter.has("initial") && !defaultCounter.has("step"));
        assertEquals(30, configuredCounter.get("initial").getAsInt());
        assertEquals(3, configuredCounter.get("step").getAsInt());
        assertEquals("invalid", wrongType.get("step").getAsString());
        assertEquals(0, wrongRange.get("step").getAsInt());
        assertEquals("anchorui:text", entries.get(5).getAsJsonObject().get("type").getAsString());
    }

    @Test
    public void importedWidgetSceneDeclaresCrossNamespaceAnchors() {
        JsonObject scene = resource(14);
        var imports = scene.getAsJsonArray("imports");
        assertEquals("anchorui_test:foreign_header", imports.get(0).getAsString());
        var elements = scene.getAsJsonArray("entities");
        assertEquals("anchorui_test:foreign_header", elements.get(0).getAsJsonObject().getAsJsonObject("position").get("relativeTo").getAsString());
        assertEquals("anchorui_test:foreign_counter", elements.get(1).getAsJsonObject().get("ref").getAsString());
        assertEquals("anchorui:local_counter", elements.get(1).getAsJsonObject().get("id").getAsString());
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
