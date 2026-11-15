package com.ludwici.anchorui.api.style;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StageThreeVisualResourcesTest {
    private JsonObject resource(String path) throws Exception {
        var stream = getClass().getClassLoader().getResourceAsStream("assets/anchorui/anchorui/" + path);
        assertNotNull(stream, "Missing resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    public void bothRpgProgressBarsInheritIdenticalFramesPaddingAndSizes() throws Exception {
        JsonObject hud = resource("hud/rpg_status.json");
        JsonObject style = resource("style/rpg_progress.json");
        assertEquals("anchorui:style/rpg_progress", hud.get("style").getAsString());
        var widgets = hud.getAsJsonArray("entities");
        JsonObject first = widgets.get(1).getAsJsonObject();
        JsonObject second = widgets.get(2).getAsJsonObject();
        assertEquals("anchorui:progress", first.get("type").getAsString());
        assertEquals("anchorui:progress", second.get("type").getAsString());
        assertEquals(first.getAsJsonObject("size"), second.getAsJsonObject("size"));
        assertEquals(first.get("showText"), second.get("showText"));
        for (JsonObject widget : new JsonObject[]{first, second}) {
            assertFalse(widget.has("borderColor"));
            assertFalse(widget.has("borderWidth"));
            assertFalse(widget.has("padding"));
            assertFalse(widget.has("background"));
        }
        LoreStyle shared = new LoreStyle(style);
        for (String key : new String[]{"borderColor", "borderWidth", "padding", "background"}) {
            var a = shared.get(ResourceLocation.parse("anchorui:progress"), first).resolvedSettings.get(key);
            var b = shared.get(ResourceLocation.parse("anchorui:progress"), second).resolvedSettings.get(key);
            assertEquals(a, b, "Different bar styling: " + key);
        }
    }

    @Test
    public void compactDemoExercisesClippedPopupAndTranslucentText() throws Exception {
        JsonObject doc = resource("screens/stage3_compact_demo.json");
        assertEquals("anchorui:style/textured_controls", doc.get("style").getAsString());
        JsonObject title = doc.getAsJsonArray("entities").get(0).getAsJsonObject();
        assertEquals(0xA0E8F5FF, ColorValues.parse(title.get("color").getAsString()));
        JsonObject panel = doc.getAsJsonArray("entities").get(1).getAsJsonObject();
        assertTrue(panel.get("clipChildren").getAsBoolean());
        var children = panel.getAsJsonArray("children");
        assertEquals("anchorui:checkbox", children.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals("anchorui:select", children.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals("anchorui:button", children.get(2).getAsJsonObject().get("type").getAsString());
        assertEquals(4, children.get(1).getAsJsonObject().getAsJsonArray("options").size());
    }
}
