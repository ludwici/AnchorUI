package com.ludwici.anchorui.api.animation;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiAnimationTriggerRegistryTest {
    @Test
    public void registeredRevisionSourceProducesIncrementingEventSequence() {
        ResourceLocation id = ResourceLocation.parse("anchorui_test:revision_source");
        AtomicLong revision = new AtomicLong();
        UiAnimationTriggerRegistry.register(id, context -> revision.get());
        assertTrue(UiAnimationTriggerRegistry.contains(id));
        assertEquals(0L, UiAnimationTriggerRegistry.sample(id, null).orElseThrow());
        revision.incrementAndGet();
        assertEquals(1L, UiAnimationTriggerRegistry.sample(id, null).orElseThrow());
        assertThrows(IllegalStateException.class, () -> UiAnimationTriggerRegistry.register(id, context -> 0L));
    }

    @Test
    public void conditionConvertsFalseAndTrueIntoRevisionValues() {
        ResourceLocation id = ResourceLocation.parse("anchorui_test:condition_source");
        java.util.concurrent.atomic.AtomicBoolean active = new java.util.concurrent.atomic.AtomicBoolean(false);
        UiAnimationTriggerRegistry.registerCondition(id, context -> active.get());
        assertEquals(0L, UiAnimationTriggerRegistry.sample(id, null).orElseThrow());
        active.set(true);
        assertEquals(1L, UiAnimationTriggerRegistry.sample(id, null).orElseThrow());
        active.set(false);
        assertEquals(0L, UiAnimationTriggerRegistry.sample(id, null).orElseThrow());
        assertFalse(UiAnimationTriggerRegistry.sample(ResourceLocation.parse("anchorui_test:unknown"), null).isPresent());
    }
}
