package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorDefaultComparisonTest {
    private static JsonObject json(String source) {
        return JsonParser.parseString(source).getAsJsonObject();
    }

    @Test
    public void layoutDefaultsUseSourceRatherThanOverridePresence() {
        String id = "demo:panel";
        JsonObject source = json("""
                {"type":"anchorui:panel","layout":{"type":"row","gap":4,"align":"center","wrap":true}}
                """);
        HudLayoutOffsets current = new HudLayoutOffsets();
        HudLayoutOffsets saved = current.copy();
        LayoutInspectorControls.Control original = findLayout(id, source, current, "layout.gap");
        assertFalse(original.modified());

        current.setSettingOverride(id, "layout", "gap", new JsonPrimitive(6));
        LayoutInspectorControls.Control changed = findLayout(id, source, current, "layout.gap");
        assertTrue(changed.modified());
        assertFalse(current.sameAs(saved));
        assertTrue(LayoutInspectorControls.sameValue("4.0", changed.sourceValue()));
        current.clearSettingOverride(id, "layout", "gap");
        assertFalse(findLayout(id, source, current, "layout.gap").modified());
        assertTrue(current.sameAs(saved));

        current.setSettingOverride(id, "layout", "gap", new JsonPrimitive(4));
        assertFalse(findLayout(id, source, current, "layout.gap").modified());
        current.clearSettingOverride(id, "layout", "gap");

        LayoutInspectorControls.Control wrap = findLayout(id, source, current, "layout.wrap");
        assertTrue(LayoutInspectorControls.sameValue("TRUE", wrap.sourceValue()));
        LayoutInspectorControls.Control layout = findLayout(id, source, current, "layout.type");
        assertTrue(LayoutInspectorControls.sameValue("anchorui:row", layout.sourceValue()));
    }

    @Test
    public void appearanceDefaultsCompareNormalizedColorsAndNumbers() {
        String id = "demo:panel";
        JsonObject source = json("""
                {"type":"anchorui:panel","fillColor":"#FF00AABB","size":{"width":200,"height":30}}
                """);
        HudLayoutOffsets current = new HudLayoutOffsets();
        HudLayoutOffsets saved = current.copy();
        AppearanceInspectorControls.Control fill = findAppearance(id, source, current, "fillColor");
        assertFalse(fill.modified());
        assertTrue(AppearanceInspectorControls.sameValue(fill.field(), "#ff00aabb", fill.sourceValue()));

        current.setSettingOverride(id, "appearance", "fillColor", new JsonPrimitive("#FFFFAABB"));
        assertTrue(findAppearance(id, source, current, "fillColor").modified());
        assertFalse(current.sameAs(saved));

        fill = findAppearance(id, source, current, "fillColor");
        assertTrue(AppearanceInspectorControls.sameValue(fill.field(), "#ff00aabb", fill.sourceValue()));
        current.clearSettingOverride(id, "appearance", "fillColor");
        assertFalse(findAppearance(id, source, current, "fillColor").modified());
        assertTrue(current.sameAs(saved));

        AppearanceInspectorControls.Control width = findAppearance(id, source, current, "size.width");
        assertTrue(AppearanceInspectorControls.sameValue(width.field(), "200", width.sourceValue()));
        assertFalse(AppearanceInspectorControls.sameValue(width.field(), "201", width.sourceValue()));
    }

    @Test
    public void revertingToPreviouslySavedCustomOverrideRestoresCleanState() {
        String id = "demo:panel";
        HudLayoutOffsets saved = new HudLayoutOffsets();
        saved.setSettingOverride(id, "layout", "gap", new JsonPrimitive(8));
        HudLayoutOffsets current = saved.copy();
        current.setSettingOverride(id, "layout", "gap", new JsonPrimitive(10));
        assertFalse(current.sameAs(saved));
        current.setSettingOverride(id, "layout", "gap", new JsonPrimitive(8));
        assertTrue(current.sameAs(saved));
    }

    private static LayoutInspectorControls.Control findLayout(String id, JsonObject source, HudLayoutOffsets state, String key) {
        return LayoutInspectorControls.describe(id, source, state).stream().filter(field -> field.key().equals(key)).findFirst().orElseThrow();
    }

    private static AppearanceInspectorControls.Control findAppearance(String id, JsonObject source, HudLayoutOffsets state, String key) {
        return AppearanceInspectorControls.describe(id, WidgetTypeIds.PANEL, source, state).stream().filter(field -> field.field().path().equals(key)).findFirst().orElseThrow();
    }
}
