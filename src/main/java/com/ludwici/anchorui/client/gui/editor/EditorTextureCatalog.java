package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** A snapshot of actual, effective PNG resources from the current client resource packs. */
public final class EditorTextureCatalog {
    public record PathKey(String pack, String namespace, String folder) {
    }

    public record Row(String label, int depth, PathKey folder, ResourceLocation texture) {
        public boolean directory() {
            return texture == null;
        }
    }

    private static final class Folder {
        private final String label;
        private final PathKey key;
        private final TreeMap<String, Folder> children = new TreeMap<>();
        private final TreeMap<String, ResourceLocation> files = new TreeMap<>();
        private Folder(String label, PathKey key) {
            this.label = label;
            this.key = key;
        }
    }

    private final List<Folder> roots;
    private final Map<ResourceLocation, PathKey> owners;
    private final int textureCount;

    private EditorTextureCatalog(List<Folder> roots, Map<ResourceLocation, PathKey> owners) {
        this.roots = List.copyOf(roots);
        this.owners = Map.copyOf(owners);
        textureCount = owners.size();
    }

    public static EditorTextureCatalog collect(ResourceManager manager) {
        List<String> packs = manager.listPacks().map(PackResources::packId).toList();
        Map<ResourceLocation, String> effective = new LinkedHashMap<>();
        for (var resource : manager.listResources("", location -> isPng(location)).entrySet()) {
            Resource texture = resource.getValue();
            effective.put(resource.getKey(), texture.sourcePackId());
        }
        return fromEntries(packs, effective);
    }

    public static boolean available(ResourceManager manager, String name) {
        ResourceLocation location = ResourceLocation.tryParse(name.trim());
        return location != null && isPng(location) && manager.getResource(location).isPresent();
    }

    public static boolean isPng(ResourceLocation location) {
        return location != null && location.getPath().endsWith(".png");
    }

    public static EditorTextureCatalog fromEntries(List<String> packIds, Map<ResourceLocation, String> effective) {
        LinkedHashMap<String, Folder> packs = new LinkedHashMap<>();
        for (String packId : packIds) {
            packs.putIfAbsent(packId, new Folder(packId, new PathKey(packId, "", "")));
        }
        List<Map.Entry<ResourceLocation, String>> files = new ArrayList<>(effective.entrySet());
        files.sort(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)));
        Map<ResourceLocation, PathKey> owners = new HashMap<>();
        for (var entry : files) {
            ResourceLocation id = entry.getKey();
            if (!isPng(id)) {
                continue;
            }
            String pack = entry.getValue();
            Folder root = packs.computeIfAbsent(pack, name -> new Folder(name, new PathKey(name, "", "")));
            String namespace = id.getNamespace();
            Folder parent = root.children.computeIfAbsent(namespace, name -> new Folder(name, new PathKey(pack, name, "")));
            String[] segments = id.getPath().split("/");
            StringBuilder folderPath = new StringBuilder();
            for (int index = 0; index < segments.length - 1; index++) {
                String segment = segments[index];
                if (!folderPath.isEmpty()) {
                    folderPath.append("/");
                }
                folderPath.append(segment);
                String path = folderPath.toString();
                Folder directory = parent;
                parent = directory.children.computeIfAbsent(segment, name -> new Folder(name, new PathKey(pack, namespace, path)));
            }
            parent.files.put(segments[segments.length - 1], id);
            owners.put(id, parent.key);
        }
        return new EditorTextureCatalog(new ArrayList<>(packs.values()), owners);
    }

    public int textureCount() {
        return textureCount;
    }

    public boolean contains(ResourceLocation id) {
        return owners.containsKey(id);
    }

    public Set<PathKey> expandedFor(ResourceLocation texture) {
        Set<PathKey> expanded = new HashSet<>();
        PathKey folder = owners.get(texture);
        if (folder == null) {
            return expanded;
        }
        expanded.add(new PathKey(folder.pack(), "", ""));
        expanded.add(new PathKey(folder.pack(), folder.namespace(), ""));
        String path = folder.folder();
        while (!path.isEmpty()) {
            expanded.add(new PathKey(folder.pack(), folder.namespace(), path));
            int slash = path.lastIndexOf('/');
            path = slash < 0 ? "" : path.substring(0, slash);
        }
        return expanded;
    }

    public List<Row> visible(Set<PathKey> expanded) {
        List<Row> lines = new ArrayList<>();
        for (Folder root : roots) {
            append(root, 0, expanded, lines);
        }
        return List.copyOf(lines);
    }

    public Set<PathKey> rootFolders() {
        Set<PathKey> paths = new HashSet<>();
        for (Folder root : roots) {
            paths.add(root.key);
        }
        return paths;
    }

    private static void append(Folder folder, int depth, Set<PathKey> expanded, List<Row> lines) {
        lines.add(new Row(folder.label, depth, folder.key, null));
        if (!expanded.contains(folder.key)) {
            return;
        }
        for (Folder child : folder.children.values()) {
            append(child, depth + 1, expanded, lines);
        }
        for (var file : folder.files.entrySet()) {
            lines.add(new Row(file.getKey(), depth + 1, null, file.getValue()));
        }
    }
}
