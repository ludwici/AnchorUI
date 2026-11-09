package com.ludwici.anchorui.api.action;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DialogueResourceTest {
    private JsonObject resource(String id) {
        String path = "assets/anchorui/anchorui/screens/" + id + ".json";
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, "Missing dialogue resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    public void dialogueButtonsHaveExplicitSafeActions() {
        JsonObject intro = resource("dialogue_intro");
        var entries = intro.getAsJsonArray("entities");
        assertEquals(4, entries.size());
        assertEquals("anchorui:button", entries.get(2).getAsJsonObject().get("type").getAsString());
        JsonObject open = entries.get(2).getAsJsonObject().getAsJsonObject("onClick");
        assertEquals("anchorui:open_screen", open.get("type").getAsString());
        assertEquals("anchorui:dialogue_next", open.get("screen").getAsString());
        JsonObject close = entries.get(3).getAsJsonObject().getAsJsonObject("onClick");
        assertEquals("anchorui:close_screen", close.get("type").getAsString());

        JsonObject second = resource("dialogue_next");
        assertEquals("anchorui:close_screen", second.getAsJsonArray("entities").get(2).getAsJsonObject().getAsJsonObject("onClick").get("type").getAsString());
        assertTrue(second.getAsJsonArray("entities").get(1).getAsJsonObject().get("text").getAsString().contains("Quest data"));
    }
}
