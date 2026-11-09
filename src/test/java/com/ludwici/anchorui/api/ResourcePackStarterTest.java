package com.ludwici.anchorui.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourcePackStarterTest {
    private static final Path STARTER = Path.of("examples", "resourcepack", "AnchorUI-Starter");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(STARTER.resolve(path), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    public void starterPackSupportsMinecraft1211() throws IOException {
        assertEquals(34, read("pack.mcmeta").getAsJsonObject("pack").get("pack_format").getAsInt());
        JsonObject status = read("assets/mypack/anchorui/hud/status.json");
        var widgets = status.getAsJsonArray("entities");
        assertEquals(3, widgets.size());
        assertEquals("anchorui:progress", widgets.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals("anchorui:player.health", widgets.get(1).getAsJsonObject().getAsJsonObject("value").get("bind").getAsString());
        assertEquals("anchorui:player.max_health", widgets.get(1).getAsJsonObject().getAsJsonObject("max").get("bind").getAsString());
        assertEquals("anchorui:player.food", widgets.get(2).getAsJsonObject().getAsJsonObject("value").get("bind").getAsString());
    }

    @Test
    public void sampleDialogueOnlyUsesApprovedNavigationActions() throws IOException {
        JsonObject intro = read("assets/mypack/anchorui/screens/traveler.json");
        JsonObject next = read("assets/mypack/anchorui/screens/traveler_next.json");
        var elements = intro.getAsJsonArray("entities");
        assertEquals("anchorui:open_screen", elements.get(2).getAsJsonObject().getAsJsonObject("onClick").get("type").getAsString());
        assertEquals("mypack:traveler_next", elements.get(2).getAsJsonObject().getAsJsonObject("onClick").get("screen").getAsString());
        assertEquals("anchorui:close_screen", elements.get(3).getAsJsonObject().getAsJsonObject("onClick").get("type").getAsString());
        assertEquals("anchorui:close_screen", next.getAsJsonArray("entities").get(2).getAsJsonObject().getAsJsonObject("onClick").get("type").getAsString());
    }

    @Test
    public void bundledResourcesDoNotUseOldLoreDirectory() throws IOException {
        try (Stream<Path> resources = Files.walk(Path.of("src", "main", "resources", "assets"))) {
            assertFalse(resources.filter(Files::isRegularFile).anyMatch(path -> path.toString().replace('\\', '/').contains("/lore/")));
        }
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/anchorui/anchorui/hud/hud1.json")));
        assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/anchorui/anchorui/test/scenes/scene14.json")));
    }
}
