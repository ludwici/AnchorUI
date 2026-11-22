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

public class UserUiStorageTest {
    @TempDir
    Path config;

    @Test
    public void localHudAndScreenHaveSeparatePersistentDocuments() throws IOException {
        ResourceLocation hud = ResourceLocation.parse("example:hud/status");
        ResourceLocation screen = ResourceLocation.parse("example:screens/menu");

        UserUiStorage.create(config, UserUiStorage.Kind.HUD, hud);
        UserUiStorage.create(config, UserUiStorage.Kind.SCREEN, screen);

        Path hudPath = config.resolve("ui/hud/example/status.json");
        Path screenPath = config.resolve("ui/screens/example/menu.json");
        assertTrue(Files.isRegularFile(hudPath));
        assertTrue(Files.isRegularFile(screenPath));
        Map<ResourceLocation, JsonElement> loaded = UserUiStorage.load(config);
        assertEquals(2, loaded.size());
        assertTrue(loaded.get(hud).getAsJsonObject().getAsJsonArray("entities").isEmpty());
        assertTrue(loaded.get(screen).getAsJsonObject().getAsJsonArray("entities").isEmpty());
        assertEquals(1, UserUiStorage.list(config, UserUiStorage.Kind.HUD).size());
        assertEquals(1, UserUiStorage.list(config, UserUiStorage.Kind.SCREEN).size());
        assertThrows(FileAlreadyExistsException.class, () -> UserUiStorage.create(config, UserUiStorage.Kind.HUD, hud));
    }

    @Test
    public void deletingScreenRemovesItsEditorOverridesButNotOtherDocuments() throws IOException {
        ResourceLocation hud = ResourceLocation.parse("example:hud/status");
        ResourceLocation screen = ResourceLocation.parse("example:screens/menu");
        UserUiStorage.create(config, UserUiStorage.Kind.HUD, hud);
        UserUiStorage.create(config, UserUiStorage.Kind.SCREEN, screen);

        Path layout = UserUiStorage.screenLayoutPath(config, screen);
        Files.createDirectories(layout.getParent());
        Files.writeString(layout, "{\"version\":1}", StandardCharsets.UTF_8);
        UserUiStorage.delete(config, UserUiStorage.Kind.SCREEN, screen);

        assertFalse(Files.exists(layout));
        assertFalse(Files.exists(UserUiStorage.path(config, UserUiStorage.Kind.SCREEN, screen)));
        assertTrue(Files.exists(UserUiStorage.path(config, UserUiStorage.Kind.HUD, hud)));
        assertEquals(1, UserUiStorage.load(config).size());
    }

    @Test
    public void readsOlderUiFilesWithoutBreakingExistingLayouts() throws IOException {
        ResourceLocation hud = ResourceLocation.parse("example:hud/old");
        Path oldFile = config.resolve("documents/hud/example/old.json");
        Files.createDirectories(oldFile.getParent());
        Files.writeString(oldFile, "{\\"entities\\":[]}", StandardCharsets.UTF_8);
        assertTrue(UserUiStorage.load(config).containsKey(hud));
        assertEquals(1, UserUiStorage.list(config, UserUiStorage.Kind.HUD).size());
        UserUiStorage.delete(config, UserUiStorage.Kind.HUD, hud);
        assertFalse(Files.exists(oldFile));
    }

    @Test
    public void rejectsUnsafePathsAndWrongDocumentKinds() {
        ResourceLocation traversal = ResourceLocation.parse("example:hud/../../escape");
        assertThrows(IllegalArgumentException.class, () -> UserUiStorage.path(config, UserUiStorage.Kind.HUD, traversal));
        ResourceLocation screen = ResourceLocation.parse("example:screens/menu");
        assertThrows(IllegalArgumentException.class, () -> UserUiStorage.path(config, UserUiStorage.Kind.HUD, screen));
    }
}
