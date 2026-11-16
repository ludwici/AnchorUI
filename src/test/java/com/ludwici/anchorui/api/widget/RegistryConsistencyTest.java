package com.ludwici.anchorui.api.widget;

import com.ludwici.anchorui.api.animation.UiAnimationTriggerRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RegistryConsistencyTest {
    @Test
    public void editorFieldRegistryRejectsDuplicateIdsAndNullRegistrationInputs() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("stage6_test", "editor_fields");
        WidgetEditorRegistry.register(id, List.of(WidgetEditorRegistry.Field.text("caption", "Caption", "")));
        IllegalStateException duplicate = assertThrows(IllegalStateException.class, () -> WidgetEditorRegistry.register(id, List.of()));
        assertTrue(duplicate.getMessage().contains(id.toString()));
        assertThrows(NullPointerException.class, () -> WidgetEditorRegistry.register(null, List.of()));
        assertThrows(NullPointerException.class, () -> WidgetEditorRegistry.register(ResourceLocation.fromNamespaceAndPath("stage6_test", "null_fields"), null));
    }

    @Test
    public void animationTriggerRegistryUsesTheSameDuplicateAndNullFailureContracts() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("stage6_test", "trigger_registration");
        UiAnimationTriggerRegistry.register(id, context -> 0L);
        IllegalStateException duplicate = assertThrows(IllegalStateException.class, () -> UiAnimationTriggerRegistry.register(id, context -> 1L));
        assertTrue(duplicate.getMessage().contains(id.toString()));
        assertThrows(NullPointerException.class, () -> UiAnimationTriggerRegistry.register(null, context -> 0L));
        assertThrows(NullPointerException.class, () -> UiAnimationTriggerRegistry.register(ResourceLocation.fromNamespaceAndPath("stage6_test", "null_source"), null));
        assertThrows(NullPointerException.class, () -> UiAnimationTriggerRegistry.registerCondition(ResourceLocation.fromNamespaceAndPath("stage6_test", "null_condition"), null));
    }
}
