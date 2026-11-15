package com.ludwici.anchorui.api.animation;

import com.google.gson.JsonParser;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiAnimationEditorTest {
    @Test
    public void trackArraysPersistAndOverrideResourceDefinitionsWithoutMutatingJson() {
        String id = "example:button";
        var original = JsonParser.parseString("""
                {"type":"anchorui:button","animations":[{"id":"source","trigger":"open","preset":"fade"}]}
                """).getAsJsonObject();
        var edited = JsonParser.parseString("""
                [
                  {"id":"first","trigger":"open","duration":500,"delay":250,"preset":"zoom"},
                  {"id":"second","trigger":"open","duration":300,"preset":"fade"},
                  {"id":"third","trigger":"show","preset":"slideLeft"}
                ]
                """).getAsJsonArray();
        HudLayoutOffsets offsets = new HudLayoutOffsets();
        offsets.setAnimationOverride(id, edited);
        var saved = HudLayoutOffsets.fromJson(offsets.toJson());
        assertTrue(saved.sameAs(offsets));
        assertNotNull(saved.animationOverride(id));
        var effective = saved.widgetSettings(id, original);
        assertEquals(3, UiAnimationSpec.parse(effective).tracks().size());
        assertEquals(250L, UiAnimationSpec.parse(effective).tracks().getFirst().delayMillis());
        assertEquals("source", UiAnimationSpec.parse(original).tracks().getFirst().id());
        saved.clearAnimationOverride(id);
        assertFalse(saved.hasAppearanceOverrides(id));
        assertEquals("source", UiAnimationSpec.parse(saved.widgetSettings(id, original)).tracks().getFirst().id());
    }
}
