package com.ludwici.anchorui.api.layout;

import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnchorLayoutCacheTest {
    private static AnchorLayout.Frame frame(String id, String target, ElementAnchor point, ElementAnchor anchor, int x, int y, int width, int height) {
        return new AnchorLayout.Frame(id, new PositionElement(point, anchor, target, x, y), width, height);
    }

    private static AnchorLayout.Frame at(String id, String target, int x, int y, int width, int height) {
        return frame(id, target, ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, x, y, width, height);
    }

    private static Map<String, AnchorLayout.Bounds> resolve(AnchorLayout.Cache cache, int width, List<AnchorLayout.Frame> frames) {
        return AnchorLayout.resolve(width, 320, frames, cache, warning -> {
            throw new AssertionError(warning);
        });
    }

    @Test
    public void reusesUnchangedFramesAndOnlyInvalidatesDependentAnchors() {
        AnchorLayout.Cache cache = new AnchorLayout.Cache();
        List<AnchorLayout.Frame> frames = List.of(at("demo:header", "screen", 10, 12, 150, 30), frame("demo:body", "demo:header", ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMRIGHT, 2, 3, 60, 20), at("demo:grandchild", "demo:body", 5, 6, 12, 9), at("demo:independent", "screen", 180, 40, 30, 20));
        Map<String, AnchorLayout.Bounds> first = resolve(cache, 500, frames);
        assertEquals(AnchorLayout.resolve(500, 320, frames), first);
        Map<String, AnchorLayout.Bounds> second = resolve(cache, 500, List.of(at("demo:header", "screen", 10, 12, 150, 30), frame("demo:body", "demo:header", ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMRIGHT, 2, 3, 60, 20), at("demo:grandchild", "demo:body", 5, 6, 12, 9), at("demo:independent", "screen", 180, 40, 30, 20)));
        for (String id : List.of("demo:header", "demo:body", "demo:grandchild", "demo:independent")) {
            assertSame(first.get(id), second.get(id));
        }

        List<AnchorLayout.Frame> shifted = List.of(at("demo:header", "screen", 15, 12, 150, 30), frames.get(1), frames.get(2), frames.get(3));
        Map<String, AnchorLayout.Bounds> third = resolve(cache, 500, shifted);
        assertEquals(AnchorLayout.resolve(500, 320, shifted), third);
        assertNotSame(second.get("demo:header"), third.get("demo:header"));
        assertNotSame(second.get("demo:body"), third.get("demo:body"));
        assertNotSame(second.get("demo:grandchild"), third.get("demo:grandchild"));
        assertSame(second.get("demo:independent"), third.get("demo:independent"));
        assertEquals(167, third.get("demo:body").x());
    }

    @Test
    public void tracksScreenResizesAndAnchorChangesWithoutLeakingStaleBounds() {
        AnchorLayout.Cache cache = new AnchorLayout.Cache();
        List<AnchorLayout.Frame> frames = List.of(frame("demo:center", "screen", ElementAnchor.CENTER, ElementAnchor.CENTER, 0, 0, 50, 24), at("demo:child", "demo:center", 3, 4, 15, 9));
        Map<String, AnchorLayout.Bounds> original = resolve(cache, 300, frames);
        Map<String, AnchorLayout.Bounds> resized = resolve(cache, 420, frames);
        assertEquals(AnchorLayout.resolve(420, 320, frames), resized);
        assertNotSame(original.get("demo:center"), resized.get("demo:center"));
        assertNotSame(original.get("demo:child"), resized.get("demo:child"));
        assertEquals(185, resized.get("demo:center").x());

        List<AnchorLayout.Frame> redirected = List.of(at("demo:center", "screen", 50, 0, 50, 24), frames.get(1));
        assertEquals(AnchorLayout.resolve(420, 320, redirected), resolve(cache, 420, redirected));
    }

    @Test
    public void handlesMissingReferencesEveryTimeAndRevisitsNewlyAvailableTargets() {
        AnchorLayout.Cache cache = new AnchorLayout.Cache();
        AnchorLayout.Frame child = at("demo:child", "demo:missing", 6, 7, 10, 8);
        List<String> warnings = new ArrayList<>();
        Map<String, AnchorLayout.Bounds> first = AnchorLayout.resolve(200, 100, List.of(child), cache, warnings::add);
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("demo:missing"));
        warnings.clear();
        Map<String, AnchorLayout.Bounds> repeated = AnchorLayout.resolve(200, 100, List.of(child), cache, warnings::add);
        assertSame(first.get("demo:child"), repeated.get("demo:child"));
        assertEquals(1, warnings.size());

        warnings.clear();
        List<AnchorLayout.Frame> resolvedFrames = List.of(child, at("demo:missing", "screen", 30, 40, 20, 20));
        Map<String, AnchorLayout.Bounds> resolved = AnchorLayout.resolve(200, 100, resolvedFrames, cache, warnings::add);
        assertTrue(warnings.isEmpty());
        assertEquals(36, resolved.get("demo:child").x());
        assertEquals(47, resolved.get("demo:child").y());
    }

    @Test
    public void cachedResolutionNeverMasksCyclesOrUnsupportedAnchorsAndKeepsLastValidSnapshot() {
        AnchorLayout.Cache cache = new AnchorLayout.Cache();
        List<AnchorLayout.Frame> valid = List.of(at("demo:a", "screen", 10, 10, 30, 30), at("demo:b", "demo:a", 4, 4, 10, 10));
        Map<String, AnchorLayout.Bounds> original = resolve(cache, 240, valid);
        List<AnchorLayout.Frame> cycle = List.of(at("demo:a", "demo:b", 10, 10, 30, 30), valid.get(1));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> resolve(cache, 240, cycle)).getMessage().contains("Circular"));
        List<AnchorLayout.Frame> removedContentTarget = List.of(at("demo:a", "screen", 10, 10, 30, 30), at("demo:b", "demo:a/__content", 4, 4, 10, 10));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> resolve(cache, 240, removedContentTarget)).getMessage().contains("Unsupported"));
        Map<String, AnchorLayout.Bounds> afterErrors = resolve(cache, 240, valid);
        assertSame(original.get("demo:a"), afterErrors.get("demo:a"));
        assertSame(original.get("demo:b"), afterErrors.get("demo:b"));
    }

    @Test
    public void evictsRemovedFramesAndSupportsExplicitReset() {
        AnchorLayout.Cache cache = new AnchorLayout.Cache();
        AnchorLayout.Frame first = at("demo:one", "screen", 10, 10, 20, 20);
        AnchorLayout.Frame second = at("demo:two", "screen", 40, 10, 20, 20);
        Map<String, AnchorLayout.Bounds> initial = resolve(cache, 240, List.of(first, second));
        Map<String, AnchorLayout.Bounds> removed = resolve(cache, 240, List.of(first));
        assertFalse(removed.containsKey("demo:two"));
        Map<String, AnchorLayout.Bounds> restored = resolve(cache, 240, List.of(first, second));
        assertNotSame(initial.get("demo:two"), restored.get("demo:two"));
        assertSame(initial.get("demo:one"), restored.get("demo:one"));
        cache.clear();
        Map<String, AnchorLayout.Bounds> cleared = resolve(cache, 240, List.of(first, second));
        assertNotSame(restored.get("demo:one"), cleared.get("demo:one"));
    }
}
