package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorTextureCatalogTest {
    private static ResourceLocation id(String value) {
        return ResourceLocation.parse(value);
    }

    @Test
    public void groupsOnlyEffectivePngFilesByActivePackNamespaceAndFolders() {
        ResourceLocation button = id("example:anchorui/ui/buttons/normal.png");
        ResourceLocation panel = id("example:anchorui/ui/panels/frame.png");
        ResourceLocation vanilla = id("minecraft:textures/gui/widgets.png");
        EditorTextureCatalog catalog = EditorTextureCatalog.fromEntries(List.of("vanilla", "example_pack"), Map.of(
                button, "example_pack",
                panel, "example_pack",
                vanilla, "vanilla",
                id("example:anchorui/ui/panels/frame.png.mcmeta"), "example_pack",
                id("example:anchorui/ui/panels/readme.txt"), "example_pack"
        ));
        assertEquals(3, catalog.textureCount());
        assertTrue(catalog.contains(button));
        assertFalse(catalog.contains(id("example:anchorui/ui/buttons/missing.png")));

        List<EditorTextureCatalog.Row> collapsed = catalog.visible(Set.of());
        assertEquals(List.of("vanilla", "example_pack"), collapsed.stream().map(EditorTextureCatalog.Row::label).toList());
        assertTrue(collapsed.stream().allMatch(EditorTextureCatalog.Row::directory));

        Set<EditorTextureCatalog.PathKey> expanded = catalog.expandedFor(panel);
        assertTrue(expanded.contains(new EditorTextureCatalog.PathKey("example_pack", "", "")));
        assertTrue(expanded.contains(new EditorTextureCatalog.PathKey("example_pack", "example", "")));
        assertTrue(expanded.contains(new EditorTextureCatalog.PathKey("example_pack", "example", "anchorui/ui/panels")));
        List<EditorTextureCatalog.Row> tree = catalog.visible(expanded);
        assertTrue(tree.stream().anyMatch(row -> row.texture() != null && row.texture().equals(panel)));
        assertFalse(tree.stream().anyMatch(row -> row.texture() != null && row.texture().equals(button)));
        assertTrue(tree.stream().anyMatch(row -> row.label().equals("panels") && row.depth() == 4));

        expanded.add(new EditorTextureCatalog.PathKey("example_pack", "example", "anchorui/ui/buttons"));
        assertTrue(catalog.visible(expanded).stream().anyMatch(row -> button.equals(row.texture())));
    }

    @Test
    public void selectedTextureUsesThePackSupplyingItsEffectiveResource() {
        ResourceLocation overridden = id("minecraft:textures/gui/icons.png");
        EditorTextureCatalog catalog = EditorTextureCatalog.fromEntries(List.of("vanilla", "resource_pack.zip"), Map.of(
                overridden, "resource_pack.zip"
        ));
        assertEquals(1, catalog.textureCount());
        assertEquals(Set.of(new EditorTextureCatalog.PathKey("resource_pack.zip", "", ""),
                new EditorTextureCatalog.PathKey("resource_pack.zip", "minecraft", ""),
                new EditorTextureCatalog.PathKey("resource_pack.zip", "minecraft", "textures"),
                new EditorTextureCatalog.PathKey("resource_pack.zip", "minecraft", "textures/gui")), catalog.expandedFor(overridden));
        assertTrue(catalog.visible(catalog.expandedFor(overridden)).stream().anyMatch(row -> overridden.equals(row.texture())));
        assertTrue(catalog.visible(Set.of(new EditorTextureCatalog.PathKey("vanilla", "", ""))).stream().noneMatch(row -> overridden.equals(row.texture())));
    }

    @Test
    public void acceptsOnlyPngResourceLocationsAndIgnoresUnknownPacks() {
        assertTrue(EditorTextureCatalog.isPng(id("mod:custom/character.png")));
        assertFalse(EditorTextureCatalog.isPng(id("mod:custom/character.png.mcmeta")));
        assertFalse(EditorTextureCatalog.isPng(id("mod:custom/character.jpg")));
        assertFalse(EditorTextureCatalog.isPng(null));
        EditorTextureCatalog catalog = EditorTextureCatalog.fromEntries(List.of("loaded"), Map.of(
                id("mod:textures/widget.png"), "loaded"
        ));
        assertEquals(Set.of(), catalog.expandedFor(id("mod:textures/nonexistent.png")));
        assertEquals(1, catalog.textureCount());
    }
}
