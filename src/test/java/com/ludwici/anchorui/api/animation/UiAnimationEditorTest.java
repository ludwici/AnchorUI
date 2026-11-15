package com.ludwici.anchorui.api.animation;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.widget.WidgetEditorRegistry;
import com.ludwici.anchorui.client.gui.editor.AppearanceInspectorControls;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiAnimationEditorTest {
    @Test
    public void animationFieldsApplyToBuiltinsAndCustomWidgetRegistrations() {
        for (String path : new String[]{"animation.open.preset", "animation.show.duration", "animation.hide.easing", "animation.close.preset"}) {
            assertNotNull(WidgetEditorRegistry.find(WidgetTypeIds.TEXT, path));
            assertNotNull(WidgetEditorRegistry.find(net.minecraft.resources.ResourceLocation.parse("example:custom"), path));
        }
    }

    @Test
    public void appearanceOverridesCreateAndPersistAnimationPresetsWithoutEditingResourcePack() {
        String id = "example:button";
        var original = JsonParser.parseString("""
                {
                  "type": "anchorui:button",
                  "text": "Run",
                  "size": {
                    "width": 120,
                    "height": 20
                  }
                }
                """).getAsJsonObject();
        HudLayoutOffsets draft = new HudLayoutOffsets();
        draft.setSettingOverride(id, "appearance", "animation.open.preset", new JsonPrimitive("fade"));
        draft.setSettingOverride(id, "appearance", "animation.open.duration", new JsonPrimitive(500));
        draft.setSettingOverride(id, "appearance", "animation.open.easing", new JsonPrimitive("easeOut"));
        var saved = HudLayoutOffsets.fromJson(draft.toJson());
        var effective = saved.widgetSettings(id, original);
        assertEquals("fade", effective.getAsJsonObject("animation").getAsJsonObject("open").get("preset").getAsString());
        assertEquals(500, UiAnimationSpec.parse(effective).track(UiAnimationSpec.Trigger.OPEN).durationMillis());
        assertEquals(0.0, UiAnimationSpec.parse(effective).track(UiAnimationSpec.Trigger.OPEN).from().opacity(), 0.00001);
        assertFalse(original.has("animation"));
        var controls = AppearanceInspectorControls.describe(id, WidgetTypeIds.BUTTON, effective, saved);
        assertTrue(controls.stream().anyMatch(c -> c.field().path().equals("animation.open.preset") && c.value().equals("fade") && c.modified()));
        saved.clearSettingOverride(id, "appearance", "animation.open.preset");
        assertFalse(saved.widgetSettings(id, original).getAsJsonObject("animation").getAsJsonObject("open").has("preset"));
    }

    @Test
    public void authoredCustomTracksAppearAsCustomInInspector() {
        String id = "example:text";
        var json = JsonParser.parseString("""
                {
                  "type": "anchorui:text",
                  "text": "Hello",
                  "animation": {
                    "open": {
                      "duration": 400,
                      "opacity": [0, 1]
                    }
                  }
                }
                """).getAsJsonObject();
        var controls = AppearanceInspectorControls.describe(id, WidgetTypeIds.TEXT, json, new HudLayoutOffsets());
        assertTrue(controls.stream().anyMatch(c -> c.field().path().equals("animation.open.preset") && c.value().equals("custom")));
        assertTrue(controls.stream().anyMatch(c -> c.field().path().equals("animation.show.preset") && c.value().equals("none")));
    }
}
