package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.condition.ConditionExpression;
import com.ludwici.anchorui.api.layout.WidgetContainers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConditionalWidgetCasesTest {
    private static JsonObject object(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static JsonArray array(String text) {
        return JsonParser.parseString(text).getAsJsonArray();
    }

    private static boolean visible(JsonObject widget, boolean ready, boolean first, boolean second) {
        return ConditionExpression.parse(widget.get("visibleWhen"), false, false)
                .evaluate((source, path) -> switch (source) {
                    case "test:ready" -> new JsonPrimitive(ready);
                    case "test:first" -> new JsonPrimitive(first);
                    case "test:second" -> new JsonPrimitive(second);
                    default -> throw new IllegalArgumentException("Unexpected source: " + source);
                });
    }

    @Test
    public void firstMatchingCaseWinsAndOtherwiseIsUsedOnlyWhenNoneMatch() {
        JsonObject document = object("""
                {"entities":[{
                  "type":"anchorui:cases","id":"status",
                  "visibleWhen":{"bind":"test:ready","fallback":false},
                  "position":{"point":"TOPLEFT","relativeTo":"screen","x":9},
                  "cases":[
                    {"when":{"bind":"test:first","fallback":false},
                     "widget":{"type":"anchorui:text","text":"First"}},
                    {"when":{"bind":"test:second","fallback":false},
                     "widget":{"type":"anchorui:text","text":"Second"}}
                  ],
                  "otherwise":{"type":"anchorui:text","text":"None"}
                }]}
                """);
        JsonArray result = WidgetDocumentResolver.resolve(document, "test", Map.of()).getAsJsonArray("entities");
        assertEquals(3, result.size());
        assertEquals("test:status/case_0", result.get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("test:status/case_1", result.get(1).getAsJsonObject().get("id").getAsString());
        assertEquals("test:status/otherwise", result.get(2).getAsJsonObject().get("id").getAsString());

        JsonObject first = result.get(0).getAsJsonObject();
        JsonObject second = result.get(1).getAsJsonObject();
        JsonObject fallback = result.get(2).getAsJsonObject();
        assertEquals(9, second.getAsJsonObject("position").get("x").getAsInt());
        assertEquals("First", first.get("text").getAsString());
        assertFalse(visible(first, false, true, true));
        assertFalse(visible(second, false, true, true));
        assertFalse(visible(fallback, false, false, false));
        assertTrue(visible(first, true, true, true));
        assertFalse(visible(second, true, true, true));
        assertFalse(visible(fallback, true, true, true));
        assertFalse(visible(first, true, false, true));
        assertTrue(visible(second, true, false, true));
        assertFalse(visible(fallback, true, false, true));
        assertFalse(visible(first, true, false, false));
        assertFalse(visible(second, true, false, false));
        assertTrue(visible(fallback, true, false, false));
        assertEquals(1, document.getAsJsonArray("entities").size());
    }

    @Test
    public void nestedCaseWidgetsSupportExternalTemplatesAndChildContainers() {
        Map<String, JsonObject> templates = Map.of("shared:selection_panel", object("""
                {"type":"anchorui:panel","size":{"width":180,"height":70},
                 "children":[{"type":"anchorui:text","text":"Nested"}]}
                """));
        JsonObject document = object("""
                {"entities":[{
                    "type":"anchorui:container","id":"root",
                    "children":[{
                      "type":"anchorui:cases","id":"switcher",
                      "cases":[
                        {"when":true,"widget":{"ref":"shared:selection_panel"}},
                        {"when":false,"widget":{"type":"anchorui:text","text":"Skipped"}}
                      ]
                    }]
                }]}
                """);

        JsonObject expanded = WidgetDocumentResolver.resolve(document, "test", templates);
        JsonArray result = expanded.getAsJsonArray("entities");
        assertEquals(4, result.size());
        JsonObject first = result.get(1).getAsJsonObject();
        assertEquals("test:switcher/case_0", first.get("id").getAsString());
        assertEquals("anchorui:panel", first.get("type").getAsString());
        assertEquals("test:root", expanded.getAsJsonObject("widgetParents")
                .get(first.get("id").getAsString()).getAsString());
        assertEquals("test:root", first.getAsJsonObject("position").get("relativeTo").getAsString());
        JsonObject child = result.get(2).getAsJsonObject();
        assertEquals("test:switcher/case_0", expanded.getAsJsonObject("widgetParents")
                .get(child.get("id").getAsString()).getAsString());
        assertEquals("test:switcher/case_0", child.getAsJsonObject("position").get("relativeTo").getAsString());
        assertEquals("test:switcher/case_1", result.get(3).getAsJsonObject().get("id").getAsString());
        assertTrue(visible(first, true, false, false));
        assertFalse(visible(result.get(3).getAsJsonObject(), true, false, false));
    }

    @Test
    public void conditionalWidgetsRejectMalformedBranchesAndNesting() {
        List<String> definitions = List.of(
                "{\"type\":\"anchorui:cases\",\"id\":\"x\",\"cases\":[]}",
                "{\"type\":\"anchorui:cases\",\"id\":\"x\",\"cases\":[{\"widget\":{\"type\":\"anchorui:text\"}}]}",
                "{\"type\":\"anchorui:cases\",\"id\":\"x\",\"cases\":[{\"when\":true}]}",
                "{\"type\":\"anchorui:cases\",\"id\":\"x\",\"cases\":[{\"when\":{\"op\":\"bad\"},\"widget\":{\"type\":\"anchorui:text\"}}]}",
                "{\"type\":\"anchorui:cases\",\"id\":\"x\",\"cases\":[{\"when\":true,\"widget\":{\"type\":\"anchorui:text\"}}],\"otherwise\":10}",
                "{\"type\":\"anchorui:cases\",\"id\":\"x\",\"children\":[],\"cases\":[{\"when\":true,\"widget\":{\"type\":\"anchorui:text\"}}]}"
        );
        for (String definition : definitions) {
            JsonObject document = new JsonObject();
            JsonArray entities = new JsonArray();
            entities.add(object(definition));
            document.add("entities", entities);
            assertThrows(IllegalArgumentException.class, () -> WidgetDocumentResolver.resolve(document, "test", Map.of()), definition);
        }
    }

    @Test
    public void templateListRootAlternativesOccupyTheSameCellAndKeepItemData() {
        JsonObject config = object("""
                {"key":"id","rowHeight":30,"gap":4,
                 "template":{
                   "type":"anchorui:cases","id":"row",
                   "cases":[
                     {"when":{"left":{"item":"level"},"op":"gte","right":10},
                      "widget":{"type":"anchorui:button","text":{"item":"title"}}}
                   ],
                   "otherwise":{"type":"anchorui:text","text":"Locked"}
                 }}
                """);
        List<TemplateListExpander.Entry> result = TemplateListExpander.expand(config, array("""
                [{"id":"one","title":"One","level":3},{"id":"two","title":"Two","level":12}]
                """), "test:list", Map.of());
        assertEquals(4, result.size());
        for (int index = 0; index < result.size(); index++) {
            JsonObject widget = result.get(index).settings();
            int rowIndex = index / 2;
            assertEquals("test:list", result.get(index).parentId());
            assertEquals("test:list", widget.getAsJsonObject("position").get("relativeTo").getAsString());
            assertEquals(34 * rowIndex, widget.getAsJsonObject("position").get("y").getAsInt());
            assertEquals(rowIndex, result.get(index).itemContext().index());
            assertEquals(rowIndex == 0 ? "one" : "two", result.get(index).itemContext().key());
        }
        assertEquals("One", result.get(0).settings().get("text").getAsString());
        assertEquals("Locked", result.get(1).settings().get("text").getAsString());
        JsonElement firstWhen = result.get(0).settings().getAsJsonObject("visibleWhen").get("left");
        assertEquals(3, firstWhen.getAsInt());
        assertEquals(result.get(0).settings().get("id"), TemplateListExpander.expand(config, array("""
                [{"id":"two","title":"Two","level":12},{"id":"one","title":"One","level":3}]
                """), "test:list", Map.of()).get(2).settings().get("id"));
    }
}
