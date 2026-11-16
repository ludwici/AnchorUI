package com.ludwici.anchorui.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class HudLayoutStorage {
    private static final Gson PRETTY_JSON = new GsonBuilder().setPrettyPrinting().create();

    private HudLayoutStorage() {
    }

    public static HudLayoutOffsets load(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            return new HudLayoutOffsets();
        }
        return HudLayoutOffsets.fromJson(JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject());
    }

    public static void save(Path path, HudLayoutOffsets offsets) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, PRETTY_JSON.toJson(offsets.toJson()) + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
