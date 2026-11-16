package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.animation.UiAnimationTriggerRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetAnimationIntegrationTest {
    @Test
    public void failingJavaTriggerDoesNotPreventOtherRegisteredTriggersFromRunning() {
        ResourceLocation broken = ResourceLocation.parse("api_audit_test:broken");
        ResourceLocation working = ResourceLocation.parse("api_audit_test:working");
        UiAnimationTriggerRegistry.register(broken, context -> {
            throw new IllegalStateException("Simulated integration failure");
        });
        AtomicLong revision = new AtomicLong();
        UiAnimationTriggerRegistry.register(working, context -> revision.get());
        WidgetAnimation animation = WidgetAnimation.parse(JsonParser.parseString("""
                {
                  "animations": [
                    {"id": "broken", "trigger": "api_audit_test:broken", "preset": "zoom", "duration": 200},
                    {"id": "working", "trigger": "api_audit_test:working", "preset": "slideLeft", "duration": 200}
                  ]
                }
                """).getAsJsonObject(), null, null);
        assertNotNull(animation);
        animation.update(true, 0L);
        assertEquals(0.0, animation.frame(0L).x(), 0.00001);
        revision.incrementAndGet();
        animation.update(true, 100000000L);
        assertEquals(-32.0, animation.frame(100000000L).x(), 0.00001);
        assertEquals(1.0, animation.frame(100000000L).scale(), 0.00001);
        assertEquals(-16.0, animation.frame(200000000L).x(), 0.00001);
        animation.update(true, 200000000L);
    }

    @Test
    public void missingTriggerCanBeRegisteredLaterWithoutRebuildingTheWidget() {
        ResourceLocation id = ResourceLocation.parse("api_audit_test:late_registration");
        WidgetAnimation animation = WidgetAnimation.parse(JsonParser.parseString("""
                {
                  "animations": [
                    {"id": "late", "trigger": "api_audit_test:late_registration", "preset": "zoom", "duration": 200}
                  ]
                }
                """).getAsJsonObject(), null, null);
        assertNotNull(animation);
        animation.update(true, 0L);
        AtomicLong revision = new AtomicLong();
        UiAnimationTriggerRegistry.register(id, context -> revision.get());
        animation.update(true, 100000000L);
        assertEquals(1.0, animation.frame(100000000L).scale(), 0.00001);
        revision.incrementAndGet();
        animation.update(true, 200000000L);
        assertEquals(0.82, animation.frame(200000000L).scale(), 0.00001);
        assertTrue(UiAnimationTriggerRegistry.contains(id));
    }
}
