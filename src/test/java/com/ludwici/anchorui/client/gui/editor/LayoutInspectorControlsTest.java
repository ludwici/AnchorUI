package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LayoutInspectorControlsTest {
    private static JsonObject json(String source) {
        return JsonParser.parseString(source).getAsJsonObject();
    }

    private static LayoutInspectorControls.Control find(List<LayoutInspectorControls.Control> controls, String key) {
        return controls.stream().filter(field -> key.equals(field.key())).findFirst().orElseThrow();
    }

    @Test
    public void supportsContainerLayoutTypeCycleFromNoneAndBack() {
        String id = "test:panel";
        JsonObject source = json("""
                {"type":"anchorui:panel","size":{"width":200,"height":80}}
                """);
        HudLayoutOffsets overrides = new HudLayoutOffsets();
        var original = LayoutInspectorControls.describe(id, source, overrides);
        assertEquals(1, original.size());
        assertEquals("none", find(original, "layout.type").value());
        JsonElement row = LayoutInspectorControls.stepped(find(original, "layout.type"), 1);
        assertEquals("row", row.getAsString());
        overrides.setSettingOverride(id, "layout", "type", row);
        assertEquals("row", find(LayoutInspectorControls.describe(id, source, overrides), "layout.type").value());
        assertEquals("0", find(LayoutInspectorControls.describe(id, source, overrides), "layout.gap").value());
        assertTrue(find(LayoutInspectorControls.describe(id, source, overrides), "layout.type").modified());
        overrides.setSettingOverride(id, "layout", "type", new JsonPrimitive("grid"));
        assertEquals("1", find(LayoutInspectorControls.describe(id, source, overrides), "layout.columns").value());
        overrides.clearSettingOverride(id, "layout", "type");
        assertFalse(find(LayoutInspectorControls.describe(id, source, overrides), "layout.type").modified());
    }

    @Test
    public void fieldSteppingHandlesBoundariesEnumsAndPrimitiveFlags() {
        var source = json("""
                {"type":"anchorui:container",
                 "layout":{"type":"grid","columns":256,"justifyItems":"end","collapseHidden":true}}
                """);
        var controls = LayoutInspectorControls.describe("test:root", source, new HudLayoutOffsets());
        assertEquals(256, LayoutInspectorControls.stepped(find(controls, "layout.columns"), 1).getAsInt());
        assertEquals(255, LayoutInspectorControls.stepped(find(controls, "layout.columns"), -1).getAsInt());
        assertEquals("stretch", LayoutInspectorControls.stepped(find(controls, "layout.justifyItems"), 1).getAsString());
        assertFalse(LayoutInspectorControls.stepped(find(controls, "layout.collapseHidden"), 1).getAsBoolean());
        assertEquals(10, LayoutInspectorControls.stepped(find(controls, "layout.cellWidth"), 1).getAsInt());
        assertThrows(IllegalArgumentException.class, () -> LayoutInspectorControls.stepped(find(controls, "layout.columns"), 0));
    }

    @Test
    public void childLayoutItemSupportsFlexBoundsAndManualOptOut() {
        var source = json("""
                {"type":"anchorui:button","__anchorui_container_parent":"test:root",
                 "layoutItem":{"grow":2,"shrink":1,"minWidth":30,"enabled":true}}
                """);
        var overrides = new HudLayoutOffsets();
        var controls = LayoutInspectorControls.describe("test:child", source, overrides);
        assertEquals("2", find(controls, "layoutItem.grow").value());
        assertEquals("30", find(controls, "layoutItem.minWidth").value());
        assertEquals("16384", find(controls, "layoutItem.maxWidth").value());
        assertEquals(3.0, LayoutInspectorControls.stepped(find(controls, "layoutItem.grow"), 1).getAsDouble());
        overrides.setSettingOverride("test:child", "layoutItem", "enabled", new JsonPrimitive(false));
        var disabled = LayoutInspectorControls.describe("test:child", source, overrides);
        assertEquals(1, disabled.size());
        assertEquals("false", find(disabled, "layoutItem.enabled").value());
        assertTrue(find(disabled, "layoutItem.enabled").modified());
        assertTrue(LayoutInspectorControls.stepped(find(disabled, "layoutItem.enabled"), 1).getAsBoolean());
    }

    @Test
    public void legacyFalseLayoutItemCanBeEnabledThroughAnOverride() {
        var source = json("""
                {"type":"anchorui:text","__anchorui_container_parent":"test:root","layoutItem":false}
                """);
        HudLayoutOffsets overrides = new HudLayoutOffsets();
        var field = find(LayoutInspectorControls.describe("test:child", source, overrides), "layoutItem.enabled");
        assertEquals("false", field.value());
        overrides.setSettingOverride("test:child", field.section(), field.field(), LayoutInspectorControls.stepped(field, 1));
        assertTrue(overrides.effectiveSettings("test:child", source)
                .getAsJsonObject("layoutItem").get("enabled").getAsBoolean());
        assertEquals("0", find(LayoutInspectorControls.describe("test:child", source, overrides), "layoutItem.grow").value());
    }

    @Test
    public void unrelatedWidgetsDoNotExposeLayoutControls() {
        assertTrue(LayoutInspectorControls.describe("test:label",
                json("""
                {"type":"anchorui:text"}
                """), new HudLayoutOffsets()).isEmpty());
    }
    @Test
    public void gridChildrenOnlyExposeOptionalSizeLimitsNotManualParticipationOrFlex() {
        var source = json("""
                {"type":"anchorui:button",
                 "layoutItem":false,
                 "size":{"width":80,"height":20}}
                """);
        var controls = LayoutInspectorControls.describe("test:child", source, new HudLayoutOffsets(), true);
        assertEquals(4, controls.size());
        assertEquals("0", find(controls, "layoutItem.minWidth").value());
        assertEquals("16384", find(controls, "layoutItem.maxWidth").value());
        assertEquals("0", find(controls, "layoutItem.minHeight").value());
        assertEquals("16384", find(controls, "layoutItem.maxHeight").value());
    }

}
