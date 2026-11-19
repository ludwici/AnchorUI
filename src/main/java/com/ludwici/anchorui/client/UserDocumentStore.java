package com.ludwici.anchorui.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ludwici.anchorui.AnchorUI;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Local, explicitly user-owned UI documents. Bundled resource-pack documents are never modified. */
public final class UserDocumentStore {
    private static final Gson PRETTY_JSON = new GsonBuilder().setPrettyPrinting().create();

    public enum Kind {
        HUD("hud", "hud/"),
        SCREEN("screens", "screens/");

        private final String directory;
        private final String prefix;

        Kind(String directory, String prefix) {
            this.directory = directory;
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }

        public String directory() {
            return directory;
        }
    }

    private UserDocumentStore() {
    }

    public static ResourceLocation resolve(Kind kind, String value) {
        if (value == null || !value.contains(":")) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        if (path.startsWith(kind.prefix())) {
            path = path.substring(kind.prefix().length());
        }
        if (path.isBlank() || path.startsWith("/") || path.endsWith("/")) {
            return null;
        }
        ResourceLocation result = ResourceLocation.tryParse(id.getNamespace() + ":" + kind.prefix() + path);
        if (result == null) {
            return null;
        }
        try {
            path(configRoot(), kind, result);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        return result;
    }

    public static Path configRoot() {
        return FMLPaths.CONFIGDIR.get().resolve("anchorui");
    }

    public static Path path(Path configRoot, Kind kind, ResourceLocation id) {
        if (!id.getPath().startsWith(kind.prefix())) {
            throw new IllegalArgumentException("Unexpected Anchor UI document kind: " + id);
        }
        String suffix = id.getPath().substring(kind.prefix().length());
        if (suffix.isBlank() || suffix.startsWith("/") || suffix.endsWith("/")) {
            throw new IllegalArgumentException("Invalid Anchor UI document ID: " + id);
        }
        Path directory = configRoot.resolve("documents").resolve(kind.directory()).resolve(id.getNamespace()).normalize();
        Path destination = directory.resolve(suffix + ".json").normalize();
        if (!destination.startsWith(directory) || destination.equals(directory)) {
            throw new IllegalArgumentException("Unsafe Anchor UI document path: " + id);
        }
        return destination;
    }

    public static Path path(Kind kind, ResourceLocation id) {
        return path(configRoot(), kind, id);
    }

    public static boolean exists(Kind kind, ResourceLocation id) {
        return Files.isRegularFile(path(kind, id));
    }

    public static void create(Kind kind, ResourceLocation id) throws IOException {
        Path file = path(kind, id);
        Files.createDirectories(file.getParent());
        JsonObject definition = new JsonObject();
        definition.add("entities", new JsonArray());
        Files.writeString(file, PRETTY_JSON.toJson(definition) + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    public static void delete(Kind kind, ResourceLocation id) throws IOException {
        Files.delete(path(kind, id));
        if (kind == Kind.SCREEN) {
            Files.deleteIfExists(screenLayoutPath(id));
        }
    }

    public static Path screenLayoutPath(ResourceLocation id) {
        if (!id.getPath().startsWith(Kind.SCREEN.prefix())) {
            throw new IllegalArgumentException("Not an Anchor UI screen: " + id);
        }
        Path root = configRoot().resolve("screens").resolve(id.getNamespace()).normalize();
        Path file = root.resolve(id.getPath() + ".json").normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Unsafe Anchor UI screen layout path: " + id);
        }
        return file;
    }

    public static Map<ResourceLocation, JsonElement> load() {
        Map<ResourceLocation, JsonElement> documents = new LinkedHashMap<>();
        for (Kind kind : Kind.values()) {
            Path root = configRoot().resolve("documents").resolve(kind.directory());
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(root)) {
                for (Path file : stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".json"))
                        .sorted().toList()) {
                    try {
                        Path relative = root.relativize(file);
                        if (relative.getNameCount() < 2) {
                            continue;
                        }
                        String namespace = relative.getName(0).toString();
                        String filename = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');
                        String name = filename.substring(0, filename.length() - ".json".length());
                        ResourceLocation id = ResourceLocation.tryParse(namespace + ":" + kind.prefix() + name);
                        if (id == null || !path(kind, id).equals(file.normalize())) {
                            AnchorUI.LOGGER.warn("Skipping invalid Anchor UI document path: {}", file);
                            continue;
                        }
                        documents.put(id, JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)));
                    } catch (IOException | RuntimeException exception) {
                        AnchorUI.LOGGER.warn("Could not load Anchor UI document {}", file, exception);
                    }
                }
            } catch (IOException exception) {
                AnchorUI.LOGGER.warn("Could not scan Anchor UI documents in {}", root, exception);
            }
        }
        return documents;
    }

    public static List<ResourceLocation> list(Kind kind) {
        List<ResourceLocation> ids = new ArrayList<>();
        for (ResourceLocation id : load().keySet()) {
            if (id.getPath().startsWith(kind.prefix())) {
                ids.add(id);
            }
        }
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        return List.copyOf(ids);
    }

    public static String display(Kind kind, ResourceLocation id) {
        return id.getNamespace() + ":" + id.getPath().substring(kind.prefix().length());
    }
}
