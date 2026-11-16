package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateListMultiAnchorTest {
    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    @Test
    public void nestedConstraintsDefaultToStructuralParentAndResolveNamespacedReferences() {
        JsonObject document = json("""
                {
                  "entities": [{
                    "type": "anchorui:panel",
                    "id": "card",
                    "children": [{
                      "type": "anchorui:text",
                      "id": "title",
                      "position": {
                        "anchors": [
                          {"point":"LEFT","anchor":"LEFT"},
                          {"point":"RIGHT","anchor":"RIGHT"}
                        ]
                      }
                    }]
                  }]
                }
                """);
        JsonObject result = WidgetDocumentResolver.resolve(document, "example", Map.of());
        JsonObject child = result.getAsJsonArray("entities").get(1).getAsJsonObject();
        JsonObject position = child.getAsJsonObject("position");
        assertFalse(position.has("relativeTo"));
        assertEquals("example:card", position.getAsJsonArray("anchors").get(0).getAsJsonObject().get("relativeTo").getAsString());
        assertEquals("example:card", position.getAsJsonArray("anchors").get(1).getAsJsonObject().get("relativeTo").getAsString());
        assertEquals("example:card", result.getAsJsonObject("widgetParents").get("example:title").getAsString());
    }

    @Test
    public void keyedRowsPreserveRootSpansAndRemapNestedAnchors() {
        JsonObject config = json("""
                {
                  "rowHeight": 30,
                  "gap": 5,
                  "template": {
                    "type":"anchorui:panel",
                    "id":"row",
                    "position":{
                      "anchors":[
                        {"point":"TOPLEFT","anchor":"TOPLEFT","x":5},
                        {"point":"TOPRIGHT","anchor":"TOPRIGHT","x":-5}
                      ]
                    },
                    "children":[{
                      "type":"anchorui:text",
                      "id":"label",
                      "position":{
                        "anchors":[
                          {"point":"LEFT","anchor":"LEFT"},
                          {"point":"RIGHT","anchor":"RIGHT"}
                        ]
                      }
                    }]
                  }
                }
                """);
        JsonArray items = JsonParser.parseString("[{\"label\":\"A\"},{\"label\":\"B\"}]").getAsJsonArray();
        List<TemplateListExpander.Entry> entries = TemplateListExpander.expand(config, items, "example:list", Map.of());
        assertEquals(4, entries.size());

        JsonObject firstRoot = entries.get(0).settings();
        JsonObject firstChild = entries.get(1).settings();
        JsonObject secondRoot = entries.get(2).settings();
        assertEquals(firstRoot.get("id").getAsString(), entries.get(1).parentId());
        assertEquals(0, firstRoot.getAsJsonObject("position").getAsJsonArray("anchors").get(0).getAsJsonObject().get("y").getAsInt());
        assertEquals(35, secondRoot.getAsJsonObject("position").getAsJsonArray("anchors").get(0).getAsJsonObject().get("y").getAsInt());
        assertEquals("example:list", firstRoot.getAsJsonObject("position").getAsJsonArray("anchors").get(1).getAsJsonObject().get("relativeTo").getAsString());
        for (int i = 0; i < 2; i++) {
            assertEquals(firstRoot.get("id").getAsString(), firstChild.getAsJsonObject("position").getAsJsonArray("anchors").get(i).getAsJsonObject().get("relativeTo").getAsString());
        }
        assertFalse(firstRoot.getAsJsonObject("position").has("relativeTo"));

        AnchorLayout.Frame list = new AnchorLayout.Frame("example:list", new PositionElement(json("""
                {"point":"TOPLEFT","anchor":"TOPLEFT","x":10,"y":10}
                """)), 200, 100);
        AnchorLayout.Frame first = new AnchorLayout.Frame(firstRoot.get("id").getAsString(), new PositionElement(firstRoot.getAsJsonObject("position")), 9, 30);
        AnchorLayout.Frame child = new AnchorLayout.Frame(firstChild.get("id").getAsString(), new PositionElement(firstChild.getAsJsonObject("position")), 8, 16);
        Map<String, AnchorLayout.Bounds> bounds = AnchorLayout.resolve(400, 300, List.of(child, first, list));
        assertEquals(new AnchorLayout.Bounds(15, 10, 190, 30), bounds.get(first.id()));
        assertEquals(190, bounds.get(child.id()).width());
        assertTrue(bounds.get(child.id()).x() >= bounds.get(first.id()).x());
    }
}
