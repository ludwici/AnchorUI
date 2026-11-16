package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InspectorSourceStyleRegressionTest {
    private static JsonObject json(String source) {
        return JsonParser.parseString(source).getAsJsonObject();
    }

    @Test
    public void previewMustNotBecomeTheDefaultUsedForResetHighlighting() {
        String id = "demo:panel";
        LoreStyle theme = new LoreStyle(json("""
                {
                  "anchorui:panel": {
                    "type": "anchorui:panel",
                    "fillColor": "#FF112233",
                    "size": {
                      "width": 200,
                      "height": 40
                    },
                    "layout": {
                      "type": "row",
                      "gap": 6
                    }
                  }
                }
                """));
        JsonObject declared = json("""
                {
                  "type": "anchorui:panel"
                }
                """);
        HudLayoutOffsets overrides = new HudLayoutOffsets();
        overrides.setSettingOverride(id, "appearance", "fillColor", new JsonPrimitive("#FF77AABB"));
        overrides.setSettingOverride(id, "appearance", "size.width", new JsonPrimitive(240));
        overrides.setSettingOverride(id, "layout", "gap", new JsonPrimitive(10));

        JsonObject pristine = theme.get(WidgetTypeIds.PANEL, declared).resolvedSettings.deepCopy();
        JsonObject preview = theme.get(WidgetTypeIds.PANEL, overrides.widgetSettings(id, declared)).resolvedSettings;
        assertEquals("#FF112233", pristine.get("fillColor").getAsString());
        assertEquals("#FF77AABB", preview.get("fillColor").getAsString());
        assertEquals(200, pristine.getAsJsonObject("size").get("width").getAsInt());
        assertEquals(240, preview.getAsJsonObject("size").get("width").getAsInt());

        var appearance = AppearanceInspectorControls.describe(id, WidgetTypeIds.PANEL, pristine, overrides);
        var fill = appearance.stream().filter(control -> control.field().path().equals("fillColor")).findFirst().orElseThrow();
        var width = appearance.stream().filter(control -> control.field().path().equals("size.width")).findFirst().orElseThrow();
        assertTrue(fill.modified());
        assertTrue(width.modified());
        assertEquals("#FF112233", fill.sourceValue());
        assertEquals("200", width.sourceValue());

        var layout = LayoutInspectorControls.describe(id, pristine, overrides);
        var gap = layout.stream().filter(control -> control.key().equals("layout.gap")).findFirst().orElseThrow();
        assertEquals("10", gap.value());
        assertEquals("6", gap.sourceValue());
        assertTrue(gap.modified());

        overrides.clearSettingOverride(id, "appearance", "fillColor");
        overrides.clearSettingOverride(id, "appearance", "size.width");
        overrides.clearSettingOverride(id, "layout", "gap");
        assertFalse(AppearanceInspectorControls.describe(id, WidgetTypeIds.PANEL, pristine, overrides).stream().anyMatch(AppearanceInspectorControls.Control::modified));
        assertFalse(LayoutInspectorControls.describe(id, pristine, overrides).stream().anyMatch(LayoutInspectorControls.Control::modified));
    }
}
