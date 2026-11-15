package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.layout.AnchorLayout;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiFocusTraversalTest {
    private static final AnchorLayout.Bounds VIEWPORT = new AnchorLayout.Bounds(0, 0, 320, 240);

    private static UiFocusTraversal.Candidate candidate(String id, boolean visible, boolean active,
                                                        AnchorLayout.Bounds bounds, AnchorLayout.Bounds clip) {
        return new UiFocusTraversal.Candidate(id, visible, active, bounds, clip);
    }

    @Test
    public void tabSkipsHiddenDisabledClippedAndOffscreenWidgets() {
        var entries = List.of(candidate("test:first", true, true, new AnchorLayout.Bounds(4, 4, 80, 20), null), candidate("test:hidden", false, true, new AnchorLayout.Bounds(8, 32, 80, 20), null), candidate("test:disabled", true, false, new AnchorLayout.Bounds(8, 64, 80, 20), null), candidate("test:clipped", true, true, new AnchorLayout.Bounds(8, 96, 80, 20), new AnchorLayout.Bounds(150, 96, 30, 20)), candidate("test:offscreen", true, true, new AnchorLayout.Bounds(-100, 5, 30, 20), null), candidate("test:last", true, true, new AnchorLayout.Bounds(10, 210, 65, 20), null));
        assertEquals("test:first", UiFocusTraversal.next(entries, VIEWPORT, null, false));
        assertEquals("test:last", UiFocusTraversal.next(entries, VIEWPORT, null, true));
        assertEquals("test:last", UiFocusTraversal.next(entries, VIEWPORT, "test:first", false));
        assertEquals("test:first", UiFocusTraversal.next(entries, VIEWPORT, "test:last", false));
        assertEquals("test:first", UiFocusTraversal.next(entries, VIEWPORT, "test:last", true));
        assertEquals("test:last", UiFocusTraversal.next(entries, VIEWPORT, "test:first", true));
    }

    @Test
    public void partialClippingRetainsReachableHitbox() {
        var candidate = candidate("test:partly_visible", true, true, new AnchorLayout.Bounds(10, 10, 80, 20), new AnchorLayout.Bounds(80, 5, 20, 30));
        assertTrue(UiFocusTraversal.reachable(candidate, VIEWPORT));
        var empty = candidate("test:empty", true, true, new AnchorLayout.Bounds(10, 10, 0, 20), null);
        assertFalse(UiFocusTraversal.reachable(empty, VIEWPORT));
    }

    @Test
    public void emptySetNeverFocusesInvisibleTargets() {
        assertNull(UiFocusTraversal.next(List.of(), VIEWPORT, null, false));
        assertNull(UiFocusTraversal.next(List.of(candidate("test:hidden", false, true, new AnchorLayout.Bounds(0, 0, 10, 10), null)), VIEWPORT, null, true));
    }
}
