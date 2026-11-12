package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateListExpanderTest {
    private static JsonObject object(String source) {
        return JsonParser.parseString(source).getAsJsonObject();
    }

    private static JsonArray array(String source) {
        return JsonParser.parseString(source).getAsJsonArray();
    }

    @Test
    public void repeatsButtonsWithScopedTextActionArgumentsAndItemData() {
        JsonObject template = object("""
                {
                  "key": "id",
                  "rowHeight": 25,
                  "gap": 3,
                  "template": {
                    "type": "anchorui:button",
                    "text": {
                      "item": "title"
                    },
                    "onClick": {
                      "type": "example:accept_quest",
                      "quest": {
                        "item": "id"
                      },
                      "amount": 1
                    }
                  }
                }
                """);
        List<TemplateListExpander.Entry> result = TemplateListExpander.expand(template, array("""
                [
                  {"id": "example:first", "title": "First quest"},
                  {"id": "example:second", "title": "Second quest"}
                ]
                """), "example:quests", Map.of());

        assertEquals(2, result.size());
        JsonObject first = result.get(0).settings();
        JsonObject second = result.get(1).settings();
        assertNotEquals(first.get("id").getAsString(), second.get("id").getAsString());
        assertTrue(first.get("id").getAsString().startsWith("example:quests/entry_"));
        assertEquals("First quest", first.get("text").getAsString());
        assertEquals("example:first", first.getAsJsonObject("onClick").get("quest").getAsString());
        assertEquals("example:quests", first.get("__anchorui_container_parent").getAsString());
        assertEquals("example:quests/__content", first.getAsJsonObject("position").get("relativeTo").getAsString());
        assertEquals(0, first.getAsJsonObject("position").get("y").getAsInt());
        assertEquals(28, second.getAsJsonObject("position").get("y").getAsInt());
        assertEquals("example:second", result.get(1).itemContext().get("id").getAsString());
        assertEquals(1, result.get(1).itemContext().index());
    }

    @Test
    public void keysRemainStableAcrossReorderingAndContentChanges() {
        JsonObject config = object("""
                {
                  "key": "id",
                  "template": {
                    "type": "anchorui:text",
                    "text": "{{item#title}}"
                  }
                }
                """);
        List<TemplateListExpander.Entry> before = TemplateListExpander.expand(config, array("""
                [
                  {"id":"example:alpha","title":"Alpha"},
                  {"id":"example:beta","title":"Beta"}
                ]
                """), "example:quests", Map.of());
        List<TemplateListExpander.Entry> after = TemplateListExpander.expand(config, array("""
                [
                  {"id":"example:beta","title":"Beta changed"},
                  {"id":"example:alpha","title":"Alpha"}
                ]
                """), "example:quests", Map.of());
        assertEquals(before.get(0).settings().get("id").getAsString(), after.get(1).settings().get("id").getAsString());
        assertEquals(before.get(1).settings().get("id").getAsString(), after.get(0).settings().get("id").getAsString());
        assertEquals("Beta changed", after.get(0).settings().get("text").getAsString());
        assertEquals(0, after.get(0).itemContext().index());
    }

    @Test
    public void nestedPanelsKeepTheirScopedLayoutAndConditionalItemFields() {
        JsonObject config = object("""
                {
                  "key": "id",
                  "template": {
                    "type": "anchorui:panel",
                    "id": "quest_row",
                    "children": [
                      {
                        "type": "anchorui:button",
                        "id": "accept",
                        "text": "{{item#title}}",
                        "enabledWhen": {
                          "left": {"item":"level"},
                          "op": "gte",
                          "right": 10
                        },
                        "onClick": "example:accept"
                      }
                    ]
                  }
                }
                """);
        List<TemplateListExpander.Entry> result = TemplateListExpander.expand(config, array("""
                [{"id":"example:dragon","title":"Kill dragon","level":12}]
                """), "example:quests", Map.of());

        assertEquals(2, result.size());
        String parent = result.get(0).settings().get("id").getAsString();
        JsonObject child = result.get(1).settings();
        assertTrue(child.get("id").getAsString().startsWith(parent + "/"));
        assertEquals(parent, child.get("__anchorui_container_parent").getAsString());
        assertEquals(parent + "/__content", child.getAsJsonObject("position").get("relativeTo").getAsString());
        assertEquals(12, child.getAsJsonObject("enabledWhen").get("left").getAsInt());
        assertEquals("Kill dragon", child.get("text").getAsString());
        assertEquals("example:accept", child.get("onClick").getAsString());
    }

    @Test
    public void itemReferencesSupportFallbackAndNestedObjectValues() {
        JsonObject config = object("""
                {
                  "key": "id",
                  "template": {
                    "type": "anchorui:button",
                    "text": {"item":"missing","fallback":"No title"},
                    "onClick": {
                      "type": "example:act",
                      "metadata": {"item":"extra"}
                    }
                  }
                }
                """);
        List<TemplateListExpander.Entry> result = TemplateListExpander.expand(config, array("""
                [{"id":"a","extra":{"gold":100}}]
                """), "example:quests", Map.of());
        JsonObject row = result.getFirst().settings();
        assertEquals("No title", row.get("text").getAsString());
        assertEquals(100, row.getAsJsonObject("onClick").getAsJsonObject("metadata").get("gold").getAsInt());
    }

    @Test
    public void validatesKeysAndEntryLimits() {
        JsonObject config = object("""
                {
                  "key":"id",
                  "template":{"type":"anchorui:button","text":"Test"}
                }
                """);
        assertThrows(IllegalArgumentException.class, () -> TemplateListExpander.expand(config, array("""
                [{"id":"duplicate"},{"id":"duplicate"}]
                """), "example:quests", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> TemplateListExpander.expand(config, array("""
                [{"title":"missing id"}]
                """), "example:quests", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> TemplateListExpander.expand(object("""
                {"maxEntries":300,"template":{"type":"anchorui:button"}}
                """), new JsonArray(), "example:quests", Map.of()));
    }

    @Test
    public void itemContextAndUiEventValuesAreDefensivelyCopied() {
        JsonObject value = object("""
                {"id":"demo:quest","data":{"status":"available"}}
                """);
        ItemContext item = new ItemContext(value, "demo:quest", 0);
        value.addProperty("id", "changed");
        assertEquals("demo:quest", item.value().getAsJsonObject().get("id").getAsString());
        item.value().getAsJsonObject().addProperty("id", "mutated");
        assertEquals("demo:quest", item.get("id").getAsString());
        assertFalse(item.get("unknown").isJsonPrimitive());
    }
}
