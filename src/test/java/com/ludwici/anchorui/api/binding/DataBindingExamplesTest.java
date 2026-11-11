package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataBindingExamplesTest {
    private JsonObject resource(String file) {
        try (var stream = getClass().getClassLoader().getResourceAsStream(file)) {
            if (stream == null) {
                throw new IllegalStateException("Missing binding test resource: " + file);
            }
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    public void demoHudExercisesTextNumbersBooleanAndStructuredLists() {
        JsonObject hud = resource("assets/anchorui/anchorui/hud/binding_demo.json");
        assertEquals("binding", hud.get("testGroup").getAsString());
        assertTrue(hud.get("testOnly").getAsBoolean());
        var entities = hud.getAsJsonArray("entities");
        assertEquals(6, entities.size());
        assertTrue(entities.get(1).getAsJsonObject().get("text").getAsString().contains("{{anchorui:player.name}}"));
        assertEquals("anchorui:world.dimension", entities.get(2).getAsJsonObject().getAsJsonObject("text").get("bind").getAsString());
        assertEquals("anchorui:player.profile", entities.get(3).getAsJsonObject().getAsJsonObject("value").get("bind").getAsString());
        assertEquals("health", entities.get(3).getAsJsonObject().getAsJsonObject("value").get("path").getAsString());
        assertEquals("anchorui:player.sneaking", entities.get(4).getAsJsonObject().getAsJsonObject("visibleWhen").get("bind").getAsString());
        assertEquals("anchorui:data_list", entities.get(5).getAsJsonObject().get("type").getAsString());
        assertEquals("name", entities.get(5).getAsJsonObject().get("itemPath").getAsString());
    }

    @Test
    public void dialogueScreenAndStarterPackUseBindings() throws IOException {
        JsonObject screen = resource("assets/anchorui/anchorui/screens/bindings.json");
        assertEquals("anchorui:player.name", screen.getAsJsonArray("entities").get(1).getAsJsonObject().getAsJsonObject("text").get("bind").getAsString());
        assertEquals("anchorui:uppercase", screen.getAsJsonArray("entities").get(1).getAsJsonObject().getAsJsonObject("text").get("format").getAsString());
        assertTrue(screen.getAsJsonArray("entities").get(2).getAsJsonObject().get("text").getAsString().contains("|anchorui:percent}}"));
        assertEquals("anchorui:uppercase", screen.getAsJsonArray("entities").get(3).getAsJsonObject().get("itemFormat").getAsString());
        var starter = Path.of("examples/resourcepack/AnchorUI-Starter/assets/mypack/anchorui/screens/bindings.json");
        assertTrue(Files.isRegularFile(starter));
        JsonObject external = JsonParser.parseString(Files.readString(starter, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("anchorui:data_list", external.getAsJsonArray("entities").get(3).getAsJsonObject().get("type").getAsString());
        assertEquals("anchorui:player.effects", external.getAsJsonArray("entities").get(3).getAsJsonObject().getAsJsonObject("items").get("bind").getAsString());
    }
}
