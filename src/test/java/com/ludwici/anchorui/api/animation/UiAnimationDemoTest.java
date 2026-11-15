package com.ludwici.anchorui.api.animation;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiAnimationDemoTest {
    @Test
    public void bundledDemoExercisesConcurrentOpenShowHideChangeCloseAndDelay() throws Exception {
        String path = "assets/anchorui/anchorui/screens/stage5_animation_demo.json";
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject screen = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("anchorui:style/default", screen.get("style").getAsString());
            assertTrue(screen.getAsJsonObject("state").get("visible").getAsBoolean());
            Set<UiAnimationSpec.Trigger> triggers = new HashSet<>();
            int count = 0;
            int delayed = 0;
            int concurrent = 0;
            for (var raw : screen.getAsJsonArray("entities")) {
                JsonObject element = raw.getAsJsonObject();
                if (!element.has("animations")) {
                    continue;
                }
                UiAnimationSpec parsed = UiAnimationSpec.parse(element);
                count++;
                long simultaneous = parsed.tracks().stream().filter(track -> track.trigger() == UiAnimationSpec.Trigger.OPEN).count();
                if (simultaneous > 1) {
                    concurrent++;
                }
                for (var track : parsed.tracks()) {
                    triggers.add(track.trigger());
                    if (track.delayMillis() > 0) {
                        delayed++;
                    }
                }
            }
            assertEquals(8, count);
            assertEquals(1, concurrent);
            assertTrue(delayed > 0);
            assertEquals(Set.of(UiAnimationSpec.Trigger.OPEN, UiAnimationSpec.Trigger.SHOW,
                    UiAnimationSpec.Trigger.HIDE, UiAnimationSpec.Trigger.CHANGE, UiAnimationSpec.Trigger.CLOSE), triggers);
        }
    }
}
