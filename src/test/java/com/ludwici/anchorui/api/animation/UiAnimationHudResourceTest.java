package com.ludwici.anchorui.api.animation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiAnimationHudResourceTest {
    @Test
    public void hudDemoHasNamedConcurrentTracksAndIndependentHealthWatch() throws Exception {
        String path = "assets/anchorui/anchorui/hud/stage5_animation_hud.json";
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject doc = JsonParser.parseReader(reader).getAsJsonObject();
            assertTrue(doc.get("testOnly").getAsBoolean());
            assertEquals("animation", doc.get("testGroup").getAsString());
            int count = 0;
            for (var entity : doc.getAsJsonArray("entities")) {
                JsonObject widget = entity.getAsJsonObject();
                UiAnimationSpec spec = UiAnimationSpec.parse(widget);
                assertTrue(spec.tracks().stream().anyMatch(track -> track.trigger() == UiAnimationSpec.Trigger.OPEN));
                if (widget.get("id").getAsString().contains("health")) {
                    assertTrue(spec.tracks().stream().anyMatch(track -> track.trigger() == UiAnimationSpec.Trigger.CHANGE && track.watch() != null));
                    assertTrue(spec.tracks().stream().anyMatch(track -> track.delayMillis() > 0));
                    assertTrue(widget.has("sizeTransition"));
                }
                if (widget.get("id").getAsString().contains("title")) {
                    assertTrue(spec.tracks().stream().anyMatch(track -> track.trigger() == UiAnimationSpec.Trigger.CUSTOM && track.customTrigger().toString().equals("anchorui:test_pulse")));
                }
                count++;
            }
            assertEquals(3, count);
        }
    }
}
