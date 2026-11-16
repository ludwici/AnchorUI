package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.style.ElementAnchor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HudLayoutStoragePrettyTest {
    @TempDir
    Path directory;

    @Test
    public void savesHudConfigAsIndentedJsonWithoutChangingValues() throws IOException {
        Path file = directory.resolve("anchorui/hud-layout.json");
        HudLayoutOffsets offsets = new HudLayoutOffsets();
        offsets.set("demo:health", 15, -6);
        offsets.setAnchor("demo:health", new HudLayoutOffsets.AnchorOverride("demo:panel", ElementAnchor.LEFT, ElementAnchor.RIGHT));
        offsets.setSettingOverride("demo:health", "layout", "gap", new JsonPrimitive(8));
        offsets.setHidden("demo:health", true);

        HudLayoutStorage.save(file, offsets);
        String saved = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(saved.startsWith("{\n  \"version\": 1,\n"));
        assertTrue(saved.contains("\n  \"offsets\": {\n"));
        assertTrue(saved.contains("\n    \"demo:health\": {\n"));
        assertTrue(saved.contains("\n      \"relativeTo\": \"demo:panel\""));
        assertTrue(saved.endsWith(System.lineSeparator()));
        assertEquals(offsets.toJson(), JsonParser.parseString(saved).getAsJsonObject());
        assertTrue(offsets.sameAs(HudLayoutStorage.load(file)));
        assertFalse(Files.exists(file.resolveSibling(file.getFileName() + ".tmp")));
    }

    @Test
    public void sameReadableFormatIsUsedForScreenConfigsAndCompactLegacyFilesStillLoad() throws IOException {
        Path screenFile = directory.resolve("anchorui/screens/demo/screens/example.json");
        HudLayoutOffsets offsets = new HudLayoutOffsets();
        offsets.set("demo:panel", -12, 5);
        HudLayoutStorage.save(screenFile, offsets);
        String saved = Files.readString(screenFile, StandardCharsets.UTF_8);
        assertTrue(saved.contains("\n    \"demo:panel\": {\n"));
        assertTrue(offsets.sameAs(HudLayoutStorage.load(screenFile)));

        Files.writeString(screenFile, "{\"version\":1,\"offsets\":{\"demo:panel\":{\"x\":-12,\"y\":5}}}", StandardCharsets.UTF_8);
        assertTrue(offsets.sameAs(HudLayoutStorage.load(screenFile)));
        HudLayoutStorage.save(screenFile, HudLayoutStorage.load(screenFile));
        assertTrue(Files.readString(screenFile, StandardCharsets.UTF_8).contains("\n  \"offsets\": {\n"));
    }
}
