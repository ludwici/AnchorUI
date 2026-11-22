package com.ludwici.anchorui.client.hud;

import com.ludwici.anchorui.AnchorUI;
import net.minecraft.resources.ResourceLocation;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class HudScopeStorage {
    private HudScopeStorage() {
    }

    public static Path root() {
        return FabricLoader.getInstance().getConfigDir().resolve("anchorui");
    }

    public static Path path(Path configRoot, ResourceLocation hudId) {
        if (!hudId.getPath().startsWith("hud/") || hudId.getPath().length() <= 4) {
            throw new IllegalArgumentException("Invalid HUD ID: " + hudId);
        }
        Path base = configRoot.resolve("hud-layouts").resolve(hudId.getNamespace()).normalize();
        String suffix = hudId.getPath().substring(4);
        Path file = base.resolve(suffix + ".json").normalize();
        if (!file.startsWith(base)) {
            throw new IllegalArgumentException("Invalid HUD path: " + hudId);
        }
        return file;
    }

    public static Path path(ResourceLocation hudId) {
        return path(root(), hudId);
    }

    public static Map<ResourceLocation, HudLayoutOffsets> loadAll(Path configRoot) {
        Map<ResourceLocation, HudLayoutOffsets> layouts = new LinkedHashMap<>();
        Path root = configRoot.resolve("hud-layouts");
        if (!Files.isDirectory(root)) {
            return layouts;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json")).sorted().toList()) {
                try {
                    Path relative = root.relativize(file);
                    if (relative.getNameCount() < 2) {
                        continue;
                    }
                    String namespace = relative.getName(0).toString();
                    String name = relative.subpath(1, relative.getNameCount()).toString().replace('\\', '/');
                    ResourceLocation hud = ResourceLocation.tryParse(namespace + ":hud/" + name.substring(0, name.length() - 5));
                    if (hud != null && path(configRoot, hud).equals(file.normalize())) {
                        layouts.put(hud, HudLayoutStorage.load(file));
                    }
                } catch (IOException | RuntimeException exception) {
                    AnchorUI.LOGGER.warn("Failed to load HUD layout {}", file, exception);
                }
            }
        } catch (IOException exception) {
            AnchorUI.LOGGER.warn("Failed to scan HUD layouts in {}", root, exception);
        }
        return layouts;
    }

    public static void save(Path configRoot, ResourceLocation hud, HudLayoutOffsets offsets) throws IOException {
        Path file = path(configRoot, hud);
        if (offsets.isEmpty()) {
            Files.deleteIfExists(file);
        } else {
            HudLayoutStorage.save(file, offsets);
        }
    }
}
