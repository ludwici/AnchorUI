package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

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
        assertEquals("example:quests", result.get(0).parentId());
        assertFalse(first.has("__anchorui_container_parent"));
        assertEquals("example:quests", first.getAsJsonObject("position").get("relativeTo").getAsString());
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
        assertEquals(parent, result.get(1).parentId());
        assertFalse(child.has("__anchorui_container_parent"));
        assertEquals(parent, child.getAsJsonObject("position").get("relativeTo").getAsString());
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
    @Test
    public void resolvesFileBackedTemplateByIdWithRowArgumentsAndConditions() {
        JsonObject fileDefinition;
        try (InputStream stream = TemplateListExpanderTest.class.getResourceAsStream("/assets/anchorui/anchorui/templates/quest_row.json")) {
            assertTrue(stream != null, "Expected file-backed template in main resources");
            fileDefinition = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
        JsonObject config = object("""
                {
                  "key": "id",
                  "template": "anchorui:quest_row"
                }
                """);
        List<TemplateListExpander.Entry> rows = TemplateListExpander.expand(config, array("""
                [
                  {"id":"example:a","title":"Find sword","level":2},
                  {"id":"example:b","title":"Fight dragon","level":9}
                ]
                """), "example:quest_panel", Map.of("anchorui:quest_row", fileDefinition));

        assertEquals(2, rows.size());
        JsonObject first = rows.get(0).settings();
        JsonObject second = rows.get(1).settings();
        assertEquals("Find sword", first.get("text").getAsString());
        assertEquals("example:a", first.getAsJsonObject("onClick").get("quest").getAsString());
        assertEquals(2, first.getAsJsonObject("enabledWhen").get("left").getAsInt());
        assertEquals("Fight dragon", second.get("text").getAsString());
        assertEquals(9, second.getAsJsonObject("enabledWhen").get("left").getAsInt());
        assertEquals("example:b", rows.get(1).itemContext().get("id").getAsString());
    }

    @Test
    public void acceptsJavaRegisteredDefinitionAndProtectsSnapshotFromMutations() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("template_registry_test", "java_row");
        JsonObject original = object("""
                {
                  "type": "anchorui:button",
                  "text": {"item": "title"},
                  "onClick": {
                    "type": "example:accept",
                    "quest": {"item":"id"}
                  }
                }
                """);
        WidgetTemplateRegistry.register(id, original);
        original.addProperty("text", "Modified after registration");

        JsonObject config = object("""
                {
                  "key": "id",
                  "template": "template_registry_test:java_row"
                }
                """);
        List<TemplateListExpander.Entry> rows = TemplateListExpander.expand(config, array("""
                [{"id":"example:quest","title":"Original title"}]
                """), "example:quest_panel", Map.of());

        JsonObject row = rows.getFirst().settings();
        assertEquals("Original title", row.get("text").getAsString());
        assertEquals("example:quest", row.getAsJsonObject("onClick").get("quest").getAsString());
        assertEquals(1, rows.getFirst().itemContext().value().getAsJsonObject().size() - 1);
        assertThrows(IllegalStateException.class, () -> WidgetTemplateRegistry.register(id, object("""
                {"type":"anchorui:button"}
                """)));
    }

    @Test
    public void supportsJavaTemplateFactoriesAndResourcePackOverrides() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("template_registry_test", "generated_row");
        AtomicInteger calls = new AtomicInteger();
        WidgetTemplateRegistry.register(id, () -> {
            calls.incrementAndGet();
            return object("""
                    {
                      "type": "anchorui:text",
                      "text": "Java: {{item#title}}"
                    }
                    """);
        });
        JsonObject config = object("""
                {
                  "key":"id",
                  "template":"template_registry_test:generated_row"
                }
                """);
        JsonArray rows = array("""
                [{"id":"example:one","title":"One"}]
                """);
        List<TemplateListExpander.Entry> fromJava = TemplateListExpander.expand(config, rows, "example:items", Map.of());
        assertEquals("Java: One", fromJava.getFirst().settings().get("text").getAsString());
        assertTrue(calls.get() > 0);

        List<TemplateListExpander.Entry> overridden = TemplateListExpander.expand(config, rows, "example:items", Map.of("template_registry_test:generated_row", object("""
                        {
                          "type":"anchorui:text",
                          "text":"Pack: {{item#title}}"
                        }
                        """)));
        assertEquals("Pack: One", overridden.getFirst().settings().get("text").getAsString());
    }

    @Test
    public void resolvesNestedChildrenAndInheritedReferencesFromNamedTemplates() {
        JsonObject parent = object("""
                {
                  "type":"anchorui:panel",
                  "children":[
                    {
                      "type":"anchorui:button",
                      "id":"accept",
                      "text":"{{item#title}}",
                      "onClick":{
                        "type":"example:accept",
                        "quest":{"item":"id"}
                      }
                    }
                  ]
                }
                """);
        JsonObject config = object("""
                {
                  "key":"id",
                  "template":"example:inherited_row"
                }
                """);
        List<TemplateListExpander.Entry> rows = TemplateListExpander.expand(config, array("""
                [{"id":"quest:one","title":"Sword"}]
                """), "example:quests", Map.of("example:base_row", parent, "example:inherited_row", object("""
                        {
                          "ref":"example:base_row",
                          "size":{"width":210}
                        }
                        """)));
        assertEquals(2, rows.size());
        String parentId = rows.getFirst().settings().get("id").getAsString();
        JsonObject child = rows.get(1).settings();
        assertEquals(parentId, rows.get(1).parentId());
        assertFalse(child.has("__anchorui_container_parent"));
        assertEquals("Sword", child.get("text").getAsString());
        assertEquals("quest:one", child.getAsJsonObject("onClick").get("quest").getAsString());
    }

    @Test
    public void rejectsUnknownAndInvalidTemplateReferencesButPreservesInlineTemplates() {
        JsonArray rows = array("""
                [{"id":"example:a","title":"A"}]
                """);
        assertThrows(WidgetDocumentResolver.UnknownWidgetDefinitionException.class, () -> TemplateListExpander.expand(object("""
                        {"key":"id","template":"example:missing"}
                        """), rows, "example:list", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> TemplateListExpander.expand(object("""
                        {"template":42}
                        """), rows, "example:list", Map.of()));

        List<TemplateListExpander.Entry> inline = TemplateListExpander.expand(object("""
                {
                  "key":"id",
                  "template":{
                    "type":"anchorui:text",
                    "text":{"item":"title"}
                  }
                }
                """), rows, "example:list", Map.of());
        assertEquals("A", inline.getFirst().settings().get("text").getAsString());
    }

}
