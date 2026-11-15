package com.ludwici.anchorui.api.layout;

import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnchorLayoutTest {
    private AnchorLayout.Frame frame(String id, ElementAnchor point, ElementAnchor anchor, String relativeTo, int x, int y, int width, int height) {
        return new AnchorLayout.Frame(id, new PositionElement(point, anchor, relativeTo, x, y), width, height);
    }

    @Test
    public void positionsAllNinePoints() {
        var frames = List.of(frame("topLeft", ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, "screen", 0, 0, 20, 10), frame("top", ElementAnchor.TOP, ElementAnchor.TOP, "screen", 0, 0, 20, 10), frame("topRight", ElementAnchor.TOPRIGHT, ElementAnchor.TOPRIGHT, "screen", 0, 0, 20, 10), frame("left", ElementAnchor.LEFT, ElementAnchor.LEFT, "screen", 0, 0, 20, 10), frame("center", ElementAnchor.CENTER, ElementAnchor.CENTER, "screen", 0, 0, 20, 10), frame("right", ElementAnchor.RIGHT, ElementAnchor.RIGHT, "screen", 0, 0, 20, 10), frame("bottomLeft", ElementAnchor.BOTTOMLEFT, ElementAnchor.BOTTOMLEFT, "screen", 0, 0, 20, 10), frame("bottom", ElementAnchor.BOTTOM, ElementAnchor.BOTTOM, "screen", 0, 0, 20, 10), frame("bottomRight", ElementAnchor.BOTTOMRIGHT, ElementAnchor.BOTTOMRIGHT, "screen", 0, 0, 20, 10));
        var result = AnchorLayout.resolve(200, 100, frames);
        assertEquals(new AnchorLayout.Bounds(0, 0, 20, 10), result.get("topLeft"));
        assertEquals(new AnchorLayout.Bounds(90, 0, 20, 10), result.get("top"));
        assertEquals(new AnchorLayout.Bounds(180, 0, 20, 10), result.get("topRight"));
        assertEquals(new AnchorLayout.Bounds(0, 45, 20, 10), result.get("left"));
        assertEquals(new AnchorLayout.Bounds(90, 45, 20, 10), result.get("center"));
        assertEquals(new AnchorLayout.Bounds(180, 45, 20, 10), result.get("right"));
        assertEquals(new AnchorLayout.Bounds(0, 90, 20, 10), result.get("bottomLeft"));
        assertEquals(new AnchorLayout.Bounds(90, 90, 20, 10), result.get("bottom"));
        assertEquals(new AnchorLayout.Bounds(180, 90, 20, 10), result.get("bottomRight"));
    }

    @Test
    public void anchorsAreIndependentOfDefinitionOrder() {
        var result = AnchorLayout.resolve(200, 100, List.of(frame("body", ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMRIGHT, "header", 10, -4, 30, 12), frame("header", ElementAnchor.CENTER, ElementAnchor.CENTER, "screen", 0, 0, 40, 20)));
        assertEquals(new AnchorLayout.Bounds(80, 40, 40, 20), result.get("header"));
        assertEquals(new AnchorLayout.Bounds(130, 56, 30, 12), result.get("body"));
    }

    @Test
    public void invalidReferencesFallBackToScreen() {
        var frames = List.of(frame("child", ElementAnchor.TOPLEFT, ElementAnchor.BOTTOMRIGHT, "body", 4, 6, 10, 10), frame("body", ElementAnchor.CENTER, ElementAnchor.CENTER, "unknown", 12, -5, 20, 10));
        var warnings = new java.util.ArrayList<String>();
        var result = AnchorLayout.resolve(200, 100, frames, warnings::add);
        assertEquals(new AnchorLayout.Bounds(102, 40, 20, 10), result.get("body"));
        assertEquals(new AnchorLayout.Bounds(126, 56, 10, 10), result.get("child"));
        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("unknown"));
    }

    @Test
    public void cyclesAreRejected() {
        var frames = List.of(frame("first", ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, "second", 0, 0, 10, 10), frame("second", ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, "first", 0, 0, 10, 10));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> AnchorLayout.resolve(200, 100, frames)).getMessage().contains("Circular"));
    }

    @Test
    public void duplicatesAreRejected() {
        var frames = List.of(frame("same", ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, "screen", 0, 0, 10, 10), frame("same", ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, "screen", 0, 0, 10, 10));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> AnchorLayout.resolve(200, 100, frames)).getMessage().contains("Duplicate"));
    }
}
