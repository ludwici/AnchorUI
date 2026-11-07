package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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
        JsonObject document = resource("assets/anchorui/lore/test/scenes/scene14.json");
        Map<String, JsonObject> definitions = new LinkedHashMap<>();
        definitions.put("anchorui_test:foreign_counter", resource("assets/anchorui_test/lore/widgets/foreign_counter.json"));
        definitions.put("anchorui_test:foreign_header", resource("assets/anchorui_test/lore/widgets/foreign_header.json"));
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
    public void qualifiedIdsAreValidatedAndScreenRemainsSpecial() {
        assertEquals("anchorui:title", WidgetIds.id("title", "anchorui"));
        assertEquals("othermod:title", WidgetIds.id("othermod:title", "anchorui"));
        assertEquals("screen", WidgetIds.reference("screen", "anchorui"));
        assertEquals("othermod:header", WidgetIds.reference("othermod:header", "anchorui"));
        assertThrows(IllegalArgumentException.class, () -> WidgetIds.id("Bad Name", "anchorui"));
        assertThrows(IllegalArgumentException.class, () -> WidgetIds.id(":missing_namespace", "anchorui"));
        assertThrows(IllegalArgumentException.class, () -> WidgetIds.id("invalid:", "anchorui"));
    }
}
