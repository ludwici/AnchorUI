package com.ludwici.anchorui.client;

import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserDocumentStoreTest {
    @TempDir
    Path config;

    @Test
    public void localHudAndScreenHaveSeparatePersistentDocuments() throws IOException {
        ResourceLocation hud = ResourceLocation.parse("example:hud/status");
        ResourceLocation screen = ResourceLocation.parse("example:screens/menu");

        UserDocumentStore.create(config, UserDocumentStore.Kind.HUD, hud);
        UserDocumentStore.create(config, UserDocumentStore.Kind.SCREEN, screen);

        Path hudPath = config.resolve("documents/hud/example/status.json");
        Path screenPath = config.resolve("documents/screens/example/menu.json");
        assertTrue(Files.isRegularFile(hudPath));
        assertTrue(Files.isRegularFile(screenPath));
        Map<ResourceLocation, JsonElement> loaded = UserDocumentStore.load(config);
        assertEquals(2, loaded.size());
        assertTrue(loaded.get(hud).getAsJsonObject().getAsJsonArray("entities").isEmpty());
        assertTrue(loaded.get(screen).getAsJsonObject().getAsJsonArray("entities").isEmpty());
        assertEquals(1, UserDocumentStore.list(config, UserDocumentStore.Kind.HUD).size());
        assertEquals(1, UserDocumentStore.list(config, UserDocumentStore.Kind.SCREEN).size());
        assertThrows(FileAlreadyExistsException.class, () -> UserDocumentStore.create(config, UserDocumentStore.Kind.HUD, hud));
    }

    @Test
    public void deletingScreenRemovesItsEditorOverridesButNotOtherDocuments() throws IOException {
        ResourceLocation hud = ResourceLocation.parse("example:hud/status");
        ResourceLocation screen = ResourceLocation.parse("example:screens/menu");
        UserDocumentStore.create(config, UserDocumentStore.Kind.HUD, hud);
        UserDocumentStore.create(config, UserDocumentStore.Kind.SCREEN, screen);

        Path layout = UserDocumentStore.screenLayoutPath(config, screen);
        Files.createDirectories(layout.getParent());
        Files.writeString(layout, "{\"version\":1}", StandardCharsets.UTF_8);
        UserDocumentStore.delete(config, UserDocumentStore.Kind.SCREEN, screen);

        assertFalse(Files.exists(layout));
        assertFalse(Files.exists(UserDocumentStore.path(config, UserDocumentStore.Kind.SCREEN, screen)));
        assertTrue(Files.exists(UserDocumentStore.path(config, UserDocumentStore.Kind.HUD, hud)));
        assertEquals(1, UserDocumentStore.load(config).size());
    }

    @Test
    public void rejectsUnsafePathsAndWrongDocumentKinds() {
        ResourceLocation traversal = ResourceLocation.parse("example:hud/../../escape");
        assertThrows(IllegalArgumentException.class, () -> UserDocumentStore.path(config, UserDocumentStore.Kind.HUD, traversal));
        ResourceLocation screen = ResourceLocation.parse("example:screens/menu");
        assertThrows(IllegalArgumentException.class, () -> UserDocumentStore.path(config, UserDocumentStore.Kind.HUD, screen));
    }
}
