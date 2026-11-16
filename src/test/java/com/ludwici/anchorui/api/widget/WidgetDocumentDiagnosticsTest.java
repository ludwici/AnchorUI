package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetDocumentDiagnosticsTest {
    @Test
    public void missingDefinitionsReportAssetPathsAndReferenceLocations() {
        JsonObject document = JsonParser.parseString("""
                {
                  "imports": ["ext:toolbar"],
                  "entities": [
                    {"ref": "ext:button", "id": "test:missing_button"},
                    {
                      "type": "anchorui:panel",
                      "id": "test:parent",
                      "children": [
                        {"ref": "ext:child"},
                        {"type": "anchorui:text", "id": "test:surviving_child"}
                      ]
                    },
                    {
                      "type": "anchorui:cases",
                      "id": "test:selector",
                      "cases": [
                        {"when": true, "widget": {"ref": "ext:branch"}}
                      ]
                    }
                  ]
                }
                """).getAsJsonObject();
        List<String> warnings = new ArrayList<>();
        JsonObject result = WidgetDocumentResolver.resolve(document, "test", Map.of(), warnings::add);

        assertEquals(4, warnings.size());
        assertTrue(warnings.get(0).contains("Import 'ext:toolbar'"));
        assertTrue(warnings.get(0).contains("assets/ext/anchorui/widgets/toolbar.json"));
        assertTrue(warnings.get(0).contains("assets/ext/anchorui/templates/toolbar.json"));
        assertTrue(warnings.get(1).contains("Entity #0 ref 'ext:button'"));
        assertTrue(warnings.get(2).contains("Child #0 of 'test:parent' ref 'ext:child'"));
        assertTrue(warnings.get(3).contains("Conditional branch 'test:selector/case_0' ref 'ext:branch'"));
        assertEquals(2, result.getAsJsonArray("entities").size());
        assertEquals("test:parent", result.getAsJsonArray("entities").get(0).getAsJsonObject().get("id").getAsString());
        assertEquals("test:surviving_child", result.getAsJsonArray("entities").get(1).getAsJsonObject().get("id").getAsString());
    }
    @Test
    public void malformedWidgetDocumentsPointToTheFailingEntityOrChild() {
        IllegalArgumentException nonObject = assertThrows(IllegalArgumentException.class, () -> WidgetDocumentResolver.resolve(JsonParser.parseString("""
                {"entities":[true]}
                """).getAsJsonObject(), "test", Map.of()));
        assertTrue(nonObject.getMessage().contains("Entity #0"));

        IllegalArgumentException missingType = assertThrows(IllegalArgumentException.class, () -> WidgetDocumentResolver.resolve(JsonParser.parseString("""
                {"entities":[{"id":"test:no_type"}]}
                """).getAsJsonObject(), "test", Map.of()));
        assertTrue(missingType.getMessage().contains("test:no_type"));
        assertTrue(missingType.getMessage().contains("'type'"));

        IllegalArgumentException invalidType = assertThrows(IllegalArgumentException.class, () -> WidgetDocumentResolver.resolve(JsonParser.parseString("""
                {"entities":[{"id":"test:bad_type","type":42}]}
                """).getAsJsonObject(), "test", Map.of()));
        assertTrue(invalidType.getMessage().contains("test:bad_type"));

        IllegalArgumentException invalidChild = assertThrows(IllegalArgumentException.class, () -> WidgetDocumentResolver.resolve(JsonParser.parseString("""
                {"entities":[{"type":"anchorui:panel","id":"parent","children":[false]}]}
                """).getAsJsonObject(), "test", Map.of()));
        assertTrue(invalidChild.getMessage().contains("Child #0 of 'test:parent'"));
    }

}
