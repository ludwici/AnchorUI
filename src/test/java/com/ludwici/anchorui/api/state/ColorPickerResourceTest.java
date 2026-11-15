package com.ludwici.anchorui.api.state;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.widget.WidgetEditorRegistry;
import com.ludwici.anchorui.client.gui.ColorPickerColor;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ColorPickerResourceTest {
    @Test
    public void bundledDemoDeclaresAColorPickerWithStringStateAndChangeEvent() throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream(
                "assets/anchorui/anchorui/screens/color_picker_demo.json");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject screen = JsonParser.parseReader(reader).getAsJsonObject();
            UiLocalState state = new UiLocalState(screen.getAsJsonObject("state"));
            JsonObject picker = screen.getAsJsonArray("entities").get(1).getAsJsonObject();
            assertEquals("anchorui:color_picker", picker.get("type").getAsString());
            assertEquals("anchorui_test:event_notice", picker.get("onChange").getAsString());
            assertEquals("selectedColor", picker.get("stateKey").getAsString());
            assertEquals(0xFFE85B60, ColorPickerColor.parse(state.get("selectedColor").getAsString()));
            assertEquals(288, picker.getAsJsonObject("size").get("width").getAsInt());
            assertTrue(WidgetEditorRegistry.fields(WidgetTypeIds.COLOR_PICKER).stream()
                    .anyMatch(field -> field.path().equals("size.width")));
        }
    }
}
