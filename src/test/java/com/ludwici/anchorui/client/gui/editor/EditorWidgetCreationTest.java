package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.LoreEntity;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorWidgetCreationTest {
    @Test
    public void allCreatableBuiltInTypesHaveIndependentValidDefinitions() {
        for (String type : EditorWidgetCreation.types()) {
            JsonObject first = EditorWidgetCreation.newWidget(type, "anchorui:editor_example_1", 6, -8);
            JsonObject second = EditorWidgetCreation.newWidget(type, "anchorui:editor_example_2", 0, 0);
            assertEquals(type, first.get("type").getAsString());
            assertEquals("anchorui:editor_example_1", first.get("id").getAsString());
            assertEquals("CENTER", first.getAsJsonObject("position").get("point").getAsString());
            assertEquals("screen", first.getAsJsonObject("position").get("relativeTo").getAsString());
            assertEquals(6, first.getAsJsonObject("position").get("x").getAsInt());
            assertEquals(-8, first.getAsJsonObject("position").get("y").getAsInt());
            assertTrue(first.getAsJsonObject("size").get("width").getAsInt() > 0);
            assertTrue(first.getAsJsonObject("size").get("height").getAsInt() > 0);
            assertEquals(ResourceLocation.parse(type), new LoreEntity.UiElement(first).type);
            first.addProperty("customState", true);
            assertFalse(second.has("customState"));
        }
    }

    @Test
    public void rejectsCreatingTypesWithoutAnEditorPreset() {
        assertThrows(IllegalArgumentException.class, () -> EditorWidgetCreation.newWidget("anchorui:missing", "anchorui:example", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> EditorWidgetCreation.newWidget("anchorui:color_picker", "anchorui:example", 0, 0));
    }
}
