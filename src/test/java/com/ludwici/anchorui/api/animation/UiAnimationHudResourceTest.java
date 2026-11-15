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
    public void hudDemoUsesNamedAnimationTestGroupAndTypedWatch() throws Exception {
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
                assertNotNull(spec.track(UiAnimationSpec.Trigger.OPEN));
                count++;
            }
            assertEquals(3, count);
        }
    }
}
