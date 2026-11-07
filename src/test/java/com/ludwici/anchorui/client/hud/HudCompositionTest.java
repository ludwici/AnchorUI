package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.PositionElement;
import com.ludwici.anchorui.api.widget.WidgetDocumentResolver;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HudCompositionTest {
    private JsonObject resource(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing test resource: " + path);
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private List<AnchorLayout.Frame> composedFrames() {
        JsonObject template = resource("assets/anchorui_test/lore/widgets/hud_header.json");
        Map<String, JsonObject> definitions = Map.of("anchorui_test:hud_header", template);
        JsonObject hudA = resource("assets/anchorui/lore/hud/hud1.json");
        JsonObject hudB = resource("assets/anchorui_test/lore/hud/hud1_extra.json");
        assertTrue(hudA.get("testOnly").getAsBoolean());
        assertTrue(hudB.get("testOnly").getAsBoolean());

        JsonObject resolvedA = WidgetDocumentResolver.resolve(hudA, "anchorui", definitions);
        JsonObject resolvedB = WidgetDocumentResolver.resolve(hudB, "anchorui_test", definitions);
        List<AnchorLayout.Frame> frames = new ArrayList<>();
        for (var document : List.of(resolvedB, resolvedA)) {
            for (var raw : document.getAsJsonArray("entities")) {
                JsonObject widget = raw.getAsJsonObject();
                JsonObject size = widget.getAsJsonObject("size");
                frames.add(new AnchorLayout.Frame(widget.get("id").getAsString(), new PositionElement(widget.getAsJsonObject("position")), size.get("width").getAsInt(), size.get("height").getAsInt()));
            }
        }
        return frames;
    }

    @Test
    public void hudReferencesResolveAcrossDocumentsAndNamespaces() {
        List<AnchorLayout.Frame> frames = composedFrames();
        assertEquals(6, frames.size());
        Collections.reverse(frames);
        Map<String, AnchorLayout.Bounds> bounds = AnchorLayout.resolve(854, 480, frames);
        assertEquals(7, bounds.size());
        AnchorLayout.Bounds header = bounds.get("anchorui_test:hud_header");
        AnchorLayout.Bounds body = bounds.get("anchorui:hud_body");
        AnchorLayout.Bounds item = bounds.get("anchorui:hud_item");
        AnchorLayout.Bounds extra = bounds.get("anchorui_test:hud_extra");
        AnchorLayout.Bounds copy = bounds.get("anchorui_test:hud_copy");
        assertEquals(18, header.x());
        assertEquals(45, header.y());
        assertEquals(header.y() + header.height() + 6, body.y());
        assertEquals(body.y() + body.height() + 7, item.y());
        assertEquals(item.x() + item.width() + 9, extra.x());
        assertEquals(item.y(), extra.y());
        assertEquals(extra.y() + extra.height() + 8, copy.y());
    }

    @Test
    public void bottomRightHudAnchorTracksGuiDimensions() {
        List<AnchorLayout.Frame> frames = composedFrames();
        AnchorLayout.Bounds small = AnchorLayout.resolve(320, 240, frames).get("anchorui_test:hud_corner");
        AnchorLayout.Bounds large = AnchorLayout.resolve(854, 480, frames).get("anchorui_test:hud_corner");
        assertEquals(320 - 16 - 128, small.x());
        assertEquals(240 - 16 - 23, small.y());
        assertEquals(854 - 16 - 128, large.x());
        assertEquals(480 - 16 - 23, large.y());
    }

    @Test
    public void importedTemplateAndRefOverrideRemainIndependent() {
        JsonObject hud = resource("assets/anchorui_test/lore/hud/hud1_extra.json");
        JsonObject template = resource("assets/anchorui_test/lore/widgets/hud_header.json");
        JsonObject expanded = WidgetDocumentResolver.resolve(hud, "anchorui_test", Map.of("anchorui_test:hud_header", template));
        JsonObject reference = expanded.getAsJsonArray("entities").get(2).getAsJsonObject();
        assertEquals("anchorui:text", reference.get("type").getAsString());
        assertEquals("anchorui_test:hud_copy", reference.get("id").getAsString());
        assertEquals("Template ref on HUD", reference.get("text").getAsString());
        assertEquals("anchorui_test:hud_extra", reference.getAsJsonObject("position").get("relativeTo").getAsString());
        assertFalse(reference.has("ref"));
        assertEquals("anchorui:text", template.get("type").getAsString());
    }
}
