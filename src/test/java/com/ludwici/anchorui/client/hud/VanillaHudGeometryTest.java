package com.ludwici.anchorui.client.hud;

import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VanillaHudGeometryTest {
    @Test
    public void providesAll25NeoForgeVanillaHudLayerIdsWithoutDuplicates() {
        assertEquals(25, VanillaHudGeometry.IDS.size());
        assertEquals(25, new HashSet<>(VanillaHudGeometry.IDS).size());
        assertTrue(VanillaHudGeometry.IDS.stream().allMatch(id -> id.startsWith("minecraft:")));
        assertTrue(VanillaHudGeometry.reserved("minecraft:hotbar"));
        assertTrue(VanillaHudGeometry.reserved("minecraft:camera_overlays"));
        assertFalse(VanillaHudGeometry.reserved("custommod:hotbar"));

        var measured = VanillaHudGeometry.measure(426, 240, new VanillaHudGeometry.State(Set.of(), 1, 0));
        assertEquals(Set.copyOf(VanillaHudGeometry.IDS), measured.keySet());
        assertTrue(measured.values().stream().noneMatch(VanillaHudGeometry.Reference::active));
    }

    @Test
    public void vanillaHudReferencesWorkAsRegularAnchorTargets() {
        var state = new VanillaHudGeometry.State(Set.of("minecraft:hotbar", "minecraft:crosshair"), 1, 0);
        var measured = VanillaHudGeometry.measure(426, 240, state);
        assertEquals(new AnchorLayout.Bounds(122, 218, 182, 22), measured.get("minecraft:hotbar").bounds());
        assertEquals(new AnchorLayout.Bounds(205, 112, 16, 16), measured.get("minecraft:crosshair").bounds());
        assertTrue(measured.get("minecraft:hotbar").active());

        List<AnchorLayout.Frame> frames = new ArrayList<>(VanillaHudGeometry.frames(measured));
        frames.add(new AnchorLayout.Frame("othermod:above_hotbar", new PositionElement(ElementAnchor.BOTTOM, ElementAnchor.TOP, "minecraft:hotbar", 0, -4), 80, 12));

        var layout = AnchorLayout.resolve(426, 240, frames);
        assertEquals(new AnchorLayout.Bounds(173, 202, 80, 12), layout.get("othermod:above_hotbar"));
        assertEquals(measured.get("minecraft:hotbar").bounds(), layout.get("minecraft:hotbar"));
    }

    @Test
    public void inactiveVanillaReferencesStillProvideStableAnchors() {
        var empty = VanillaHudGeometry.measure(400, 240, new VanillaHudGeometry.State(Set.of(), 1, 0));
        var layout = AnchorLayout.resolve(400, 240, VanillaHudGeometry.frames(empty));
        assertTrue(layout.containsKey("minecraft:boss_overlay"));
        assertTrue(layout.containsKey("minecraft:tab_list"));
        assertFalse(empty.get("minecraft:boss_overlay").active());
    }

    @Test
    public void referenceBoundsFollowGuiSizeAndHealthRows() {
        var state = new VanillaHudGeometry.State(Set.of("minecraft:player_health"), 3, 6);
        var first = VanillaHudGeometry.measure(400, 240, state);
        var second = VanillaHudGeometry.measure(600, 300, state);
        assertEquals(109, first.get("minecraft:player_health").bounds().x());
        assertEquals(181, first.get("minecraft:player_health").bounds().y());
        assertEquals(29, first.get("minecraft:player_health").bounds().height());
        assertEquals(209, second.get("minecraft:player_health").bounds().x());
        assertEquals(241, second.get("minecraft:player_health").bounds().y());
        assertEquals(52, first.get("minecraft:effects").bounds().height());
        assertEquals(5, first.get("minecraft:experience_bar").bounds().height());
    }
}
