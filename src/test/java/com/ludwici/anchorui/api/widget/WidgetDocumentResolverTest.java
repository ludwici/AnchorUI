package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetDocumentResolverTest {
    private JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private JsonObject resource(String path) {
        try (var stream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test resource " + path);
            }
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    public void scopesShortIdentifiersAndReferencesToDocumentNamespace() {
        JsonObject document = json("""
                {"entities":[
                    {"type":"anchorui:text","id":"body","position":{"relativeTo":"header"}},
                    {"type":"anchorui:text","id":"header","position":{"relativeTo":"screen"}}
                ]}
                """);
        JsonObject result = WidgetDocumentResolver.resolve(document, "examplemod", Map.of());
        assertEquals("examplemod:body", result.getAsJsonArray("entities").get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("examplemod:header", result.getAsJsonArray("entities").get(0).getAsJsonObject().getAsJsonObject("position").get("relativeTo").getAsString());
        assertEquals("examplemod:header", result.getAsJsonArray("entities").get(1).getAsJsonObject().get("id").getAsString());
        assertEquals("screen", result.getAsJsonArray("entities").get(1).getAsJsonObject().getAsJsonObject("position").get("relativeTo").getAsString());
    }

    @Test
    public void resolvesCrossNamespaceTemplatesRegardlessOfRegistrationOrder() {
        JsonObject document = resource("assets/anchorui/anchorui/test/scenes/scene14.json");
        Map<String, JsonObject> definitions = new LinkedHashMap<>();
        definitions.put("anchorui_test:foreign_counter", resource("assets/anchorui_test/anchorui/widgets/foreign_counter.json"));
        definitions.put("anchorui_test:foreign_header", resource("assets/anchorui_test/anchorui/widgets/foreign_header.json"));
        JsonObject expanded = WidgetDocumentResolver.resolve(document, "anchorui", definitions);
        var elements = expanded.getAsJsonArray("entities");
        assertEquals(4, elements.size());
        assertEquals("anchorui_test:foreign_header", elements.get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("anchorui:explanation", elements.get(1).getAsJsonObject().get("id").getAsString());
        assertEquals("anchorui_test:foreign_header", elements.get(1).getAsJsonObject().getAsJsonObject("position").get("relativeTo").getAsString());
        assertEquals("anchorui:local_counter", elements.get(2).getAsJsonObject().get("id").getAsString());
        assertEquals("anchorui_test:counter", elements.get(2).getAsJsonObject().get("type").getAsString());
        assertEquals(12, elements.get(2).getAsJsonObject().get("initial").getAsInt());
        assertEquals(3, elements.get(2).getAsJsonObject().get("step").getAsInt());
        assertEquals("anchorui:explanation", elements.get(2).getAsJsonObject().getAsJsonObject("position").get("relativeTo").getAsString());
        assertEquals("anchorui:footer", elements.get(3).getAsJsonObject().get("id").getAsString());
        assertFalse(expanded.has("imports"));
        assertFalse(document.getAsJsonArray("entities").get(1).getAsJsonObject().has("type"));
    }

    @Test
    public void resolvesDerivedDefinitionsAcrossNamespaces() {
        Map<String, JsonObject> definitions = new LinkedHashMap<>();
        definitions.put("othermod:derived", json("""
                {"ref":"base:widget","text":"Override","position":{"relativeTo":"header"}}
                """));
        definitions.put("base:widget", json("""
                {"type":"anchorui:text","text":"Base","size":{"width":120,"height":20},"position":{"point":"TOPLEFT","relativeTo":"screen"}}
                """));
        JsonObject doc = json("""
                {"imports":["othermod:derived"]}
                """);
        JsonObject result = WidgetDocumentResolver.resolve(doc, "screenmod", definitions);
        JsonObject widget = result.getAsJsonArray("entities").get(0).getAsJsonObject();
        assertEquals("othermod:derived", widget.get("id").getAsString());
        assertEquals("anchorui:text", widget.get("type").getAsString());
        assertEquals("Override", widget.get("text").getAsString());
        assertEquals(120, widget.getAsJsonObject("size").get("width").getAsInt());
        assertEquals("othermod:header", widget.getAsJsonObject("position").get("relativeTo").getAsString());
        assertEquals("TOPLEFT", widget.getAsJsonObject("position").get("point").getAsString());
    }

    @Test
    public void importedWidgetsCanAnchorAcrossNamespacesAndIgnoreJsonOrder() {
        Map<String, JsonObject> templates = Map.of(
                "anchorui_test:foreign_header", resource("assets/anchorui_test/anchorui/widgets/foreign_header.json"),
                "anchorui_test:foreign_counter", resource("assets/anchorui_test/anchorui/widgets/foreign_counter.json")
        );
        JsonObject expanded = WidgetDocumentResolver.resolve(resource("assets/anchorui/anchorui/test/scenes/scene14.json"), "anchorui", templates);
        List<AnchorLayout.Frame> frames = new ArrayList<>();
        for (var entry : expanded.getAsJsonArray("entities")) {
            JsonObject widget = entry.getAsJsonObject();
            JsonObject position = widget.getAsJsonObject("position");
            JsonObject size = widget.getAsJsonObject("size");
            var point = ElementAnchor.fromString(position.get("point").getAsString());
            var anchor = ElementAnchor.fromString(position.get("anchor").getAsString());
            String relativeTo = position.get("relativeTo").getAsString();
            int x = position.get("x").getAsInt();
            int y = position.get("y").getAsInt();
            frames.add(new AnchorLayout.Frame(widget.get("id").getAsString(), new PositionElement(point, anchor, relativeTo, x, y),
                    size.get("width").getAsInt(), size.get("height").getAsInt()));
        }
        Collections.reverse(frames);
        Map<String, AnchorLayout.Bounds> layout = AnchorLayout.resolve(854, 480, frames);
        var header = layout.get("anchorui_test:foreign_header");
        var explanation = layout.get("anchorui:explanation");
        var counter = layout.get("anchorui:local_counter");
        var footer = layout.get("anchorui:footer");
        assertEquals(header.y() + header.height() + 11, explanation.y());
        assertEquals(explanation.y() + explanation.height() + 13, counter.y());
        assertEquals(counter.y() + counter.height() + 10, footer.y());
    }

    @Test
    public void duplicateQualifiedIdentifiersAreRejected() {
        JsonObject doc = json("""
                {"imports":["othermod:header"],"entities":[{"type":"anchorui:text","id":"othermod:header"}]}
                """);
        Map<String, JsonObject> templates = Map.of("othermod:header", json("""
                {"type":"anchorui:text"}
                """));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> WidgetDocumentResolver.resolve(doc, "my_mod", templates));
        assertTrue(error.getMessage().contains("Duplicate widget id: othermod:header"));
    }

    @Test
    public void missingAndCircularDefinitionsAreReported() {
        JsonObject doc = json("""
                {"imports":["moda:first"]}
                """);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> WidgetDocumentResolver.resolve(doc, "modb", Map.of())).getMessage().contains("moda:first"));
        Map<String, JsonObject> templates = Map.of(
                "moda:first", json("{\"ref\":\"modb:second\"}"),
                "modb:second", json("{\"ref\":\"moda:first\"}")
        );
        assertTrue(assertThrows(IllegalArgumentException.class, () -> WidgetDocumentResolver.resolve(doc, "modb", templates)).getMessage().contains("Circular widget definitions"));
    }

    @Test
    public void missingOptionalDefinitionsSkipOnlyMissingWidgets() {
        JsonObject document = json("""
                {"imports":["missingmod:header"],"entities":[
                    {"ref":"missingmod:button","id":"consumer:missing"},
                    {"type":"anchorui:text","id":"consumer:survives"}
                ]}
                """);
        List<String> warnings = new ArrayList<>();
        JsonObject result = WidgetDocumentResolver.resolve(document, "consumer", Map.of(), warnings::add);
        assertTrue(result.has("entities"));
        assertEquals(1, result.getAsJsonArray("entities").size());
        assertEquals("consumer:survives", result.getAsJsonArray("entities").get(0).getAsJsonObject().get("id").getAsString());
        assertEquals(2, warnings.size());
        assertTrue(warnings.get(0).contains("missingmod:header"));
        assertTrue(warnings.get(1).contains("missingmod:button"));
    }

    @Test
    public void legacyDocumentsKeepTheirOriginalFormat() {
        JsonObject legacy = json("""
                {"title":"Legacy title","lore":[{"desc":"Legacy lore"}]}
                """);
        JsonObject result = WidgetDocumentResolver.resolve(legacy, "anchorui", Map.of());
        assertFalse(result.has("entities"));
        JsonObject emptyImports = json("""
                {"imports":["missingmod:widget"]}
                """);
        JsonObject expanded = WidgetDocumentResolver.resolve(emptyImports, "anchorui", Map.of(), warning -> {});
        assertTrue(expanded.has("entities"));
        assertEquals(0, expanded.getAsJsonArray("entities").size());
    }

    @Test
    public void qualifiedIdsAreValidatedAndScreenRemainsSpecial() {
        assertEquals("anchorui:title", WidgetIds.id("title", "anchorui"));
        assertEquals("othermod:title", WidgetIds.id("othermod:title", "anchorui"));
        assertEquals("screen", WidgetIds.reference("screen", "anchorui"));
        assertEquals("othermod:header", WidgetIds.reference("othermod:header", "anchorui"));
        assertThrows(IllegalArgumentException.class, () -> WidgetIds.id("screen", "anchorui"));
        assertThrows(IllegalArgumentException.class, () -> WidgetIds.id("Bad Name", "anchorui"));
        assertThrows(IllegalArgumentException.class, () -> WidgetIds.id(":missing_namespace", "anchorui"));
        assertThrows(IllegalArgumentException.class, () -> WidgetIds.id("invalid:", "anchorui"));
    }
    @Test
    public void gridChildrenOnlyNeedTheChildrenArrayInAuthoredJson() {
        JsonObject document = json("""
                {
                  "entities": [
                    {
                      "type": "anchorui:panel",
                      "id": "grid",
                      "layout": {"type":"grid","columns":3,"gap":6},
                      "children": [
                        {"type":"anchorui:button","id":"first","text":"First"},
                        {"type":"anchorui:button","id":"second","text":"Second"},
                        {"type":"anchorui:button","id":"third","text":"Third"}
                      ]
                    }
                  ]
                }
                """);
        JsonObject result = WidgetDocumentResolver.resolve(document, "example", Map.of());
        assertEquals(4, result.getAsJsonArray("entities").size());
        assertEquals(3, result.getAsJsonObject("widgetParents").size());
        for (int index = 1; index <= 3; index++) {
            JsonObject child = result.getAsJsonArray("entities").get(index).getAsJsonObject();
            assertEquals("example:grid", result.getAsJsonObject("widgetParents")
                    .get(child.get("id").getAsString()).getAsString());
            assertEquals("example:grid",
                    child.getAsJsonObject("position").get("relativeTo").getAsString());
            assertFalse(child.has("__anchorui_container_parent"));
            assertFalse(child.has("layoutItem"));
        }
        JsonObject sourceChild = document.getAsJsonArray("entities").get(0)
                .getAsJsonObject().getAsJsonArray("children").get(0).getAsJsonObject();
        assertFalse(sourceChild.has("position"));
        assertFalse(sourceChild.has("__anchorui_container_parent"));
        assertFalse(sourceChild.has("layoutItem"));
    }

    @Test
    public void bundledAnchorDemoLoadsWithValidNamespacedModelId() {
        JsonObject document = resource("assets/anchorui/anchorui/test/anchors.json");
        JsonObject expanded = WidgetDocumentResolver.resolve(document, "anchorui", Map.of());
        var elements = expanded.getAsJsonArray("entities");
        assertEquals(3, elements.size());
        assertEquals("anchorui:item_model", elements.get(1).getAsJsonObject().get("id").getAsString());
        assertEquals("anchorui:item_model",
                elements.get(2).getAsJsonObject().getAsJsonObject("position").get("relativeTo").getAsString());
    }

    @Test
    public void removedContentAnchorIsRejectedInsteadOfSilentlyFallingBack() {
        JsonObject document = json("""
                {"entities":[
                  {"type":"anchorui:panel","id":"panel"},
                  {"type":"anchorui:text","id":"child","position":{"relativeTo":"panel/__content"}}
                ]}
                """);
        assertThrows(IllegalArgumentException.class,
                () -> WidgetDocumentResolver.resolve(document, "test", Map.of()));
    }

}
