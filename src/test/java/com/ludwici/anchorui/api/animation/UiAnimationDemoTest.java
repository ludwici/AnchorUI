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
    public void bundledDemoExercisesOpenShowHideChangeAndClose() throws Exception {
        String path = "assets/anchorui/anchorui/screens/stage5_animation_demo.json";
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject screen = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("anchorui:style/default", screen.get("style").getAsString());
            assertTrue(screen.getAsJsonObject("state").get("visible").getAsBoolean());
            Set<UiAnimationSpec.Trigger> triggers = new HashSet<>();
            int animatedWidgets = 0;
            for (var raw : screen.getAsJsonArray("entities")) {
                JsonObject element = raw.getAsJsonObject();
                if (!element.has("animation")) {
                    continue;
                }
                UiAnimationSpec animation = UiAnimationSpec.parse(element);
                animatedWidgets++;
                for (var trigger : UiAnimationSpec.Trigger.values()) {
                    if (animation.track(trigger) != null) {
                        triggers.add(trigger);
                    }
                }
            }
            assertEquals(8, animatedWidgets);
            assertEquals(Set.of(UiAnimationSpec.Trigger.OPEN, UiAnimationSpec.Trigger.SHOW,
                    UiAnimationSpec.Trigger.HIDE, UiAnimationSpec.Trigger.CHANGE,
                    UiAnimationSpec.Trigger.CLOSE), triggers);
        }
    }
}
