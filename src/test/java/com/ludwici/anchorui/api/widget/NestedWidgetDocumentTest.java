package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NestedWidgetDocumentTest {
    @Test
    public void expandsNestedPanelsAndRelativeContentReferences() {
        var document = JsonParser.parseString("""
                {"entities":[{"type":"anchorui:container","id":"example:outer","children":[
                    {"type":"anchorui:text","id":"title","text":"One"},
                    {"type":"anchorui:panel","id":"panel","children":[{"type":"anchorui:text","text":"Two"}]}
                ]}]}
                """).getAsJsonObject();
        var result = WidgetDocumentResolver.resolve(document, "example", Map.of()).getAsJsonArray("entities");
        assertEquals(4, result.size());
        assertEquals("example:outer", result.get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("example:outer/__content", result.get(1).getAsJsonObject().getAsJsonObject("position").get("relativeTo").getAsString());
        assertEquals("example:panel/__content", result.get(3).getAsJsonObject().getAsJsonObject("position").get("relativeTo").getAsString());
        assertEquals("example:panel/child_0", result.get(3).getAsJsonObject().get("id").getAsString());
        assertEquals("example:panel", result.get(3).getAsJsonObject().get("__anchorui_container_parent").getAsString());
    }

    @Test
    public void childrenCannotBeAttachedToPlainWidgets() {
        var document = JsonParser.parseString("""
                {"entities":[{"type":"anchorui:text","id":"plain","children":[{"type":"anchorui:text","text":"oops"}]}]}
                """).getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> WidgetDocumentResolver.resolve(document, "test", Map.of()));
    }

    @Test
    public void nestedTemplateReferenceCanBeOverridden() {
        var document = JsonParser.parseString("""
                {"entities":[{"type":"anchorui:container","id":"box","children":[{"ref":"mod:label","id":"own","text":"Custom"}]}]}
                """).getAsJsonObject();
        var template = JsonParser.parseString("""
                {"type":"anchorui:text","text":"Original"}
                """).getAsJsonObject();
        var result = WidgetDocumentResolver.resolve(document, "demo", Map.of("mod:label", template)).getAsJsonArray("entities");
        assertEquals("demo:own", result.get(1).getAsJsonObject().get("id").getAsString());
        assertEquals("Custom", result.get(1).getAsJsonObject().get("text").getAsString());
        assertTrue(result.get(1).getAsJsonObject().has("__anchorui_container_parent"));
    }
}
