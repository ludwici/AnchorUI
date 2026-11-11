package com.ludwici.anchorui.api.layout;

import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetContainersTest {
    @Test
    public void innerContentFrameRespectsPaddingAndNestedAnchors() {
        var outer = new AnchorLayout.Frame("demo:outer", new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, "screen", 70, 80), 180, 120);
        var content = WidgetContainers.contentFrame("demo:outer", 180, 120, new PaddingElement(10, 12, 14, 16));
        var child = new AnchorLayout.Frame("demo:child", new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, WidgetContainers.contentId("demo:outer"), 0, 0), 40, 20);
        var result = AnchorLayout.resolve(400, 300, List.of(outer, content, child));
        assertEquals(new AnchorLayout.Bounds(80, 92, 156, 92), result.get(WidgetContainers.contentId("demo:outer")));
        assertEquals(new AnchorLayout.Bounds(80, 92, 40, 20), result.get("demo:child"));
    }

    @Test
    public void doesNotClipWhenParentHasNotOptedIn() {
        var bounds = Map.of("screen", new AnchorLayout.Bounds(0, 0, 300, 200),
                "demo:outer/__content", new AnchorLayout.Bounds(10, 10, 30, 30));
        var parents = Map.of("demo:child", "demo:outer");
        assertTrue(WidgetContainers.clips(bounds, parents, java.util.Set.of()).isEmpty());
    }

    @Test
    public void onlyOptedInAncestorsClipTheirDescendants() {
        var bounds = Map.of("screen", new AnchorLayout.Bounds(0, 0, 300, 200),
                "demo:outer/__content", new AnchorLayout.Bounds(10, 10, 30, 30),
                "demo:inner/__content", new AnchorLayout.Bounds(15, 15, 100, 100));
        var parents = Map.of("demo:inner", "demo:outer", "demo:leaf", "demo:inner");
        var clips = WidgetContainers.clips(bounds, parents, java.util.Set.of("demo:outer"));
        assertEquals(new AnchorLayout.Bounds(10, 10, 30, 30), clips.get("demo:leaf"));
    }

    @Test
    public void optedInClipUsesScaledContentBounds() {
        var bounds = Map.of(
                "screen", new AnchorLayout.Bounds(0, 0, 400, 200),
                "demo:box", new AnchorLayout.Bounds(80, 40, 200, 100),
                "demo:box/__content", new AnchorLayout.Bounds(90, 50, 180, 80),
                "demo:leaf", new AnchorLayout.Bounds(90, 50, 30, 20));
        var parents = Map.of("demo:leaf", "demo:box");
        var transforms = WidgetTransforms.resolve(bounds, parents,
                Map.of("demo:box", new WidgetTransforms.Group(0.5, false)));
        var clips = WidgetContainers.clips(bounds, parents, java.util.Set.of("demo:box"), transforms);
        assertEquals(new AnchorLayout.Bounds(85, 45, 90, 40), clips.get("demo:leaf"));
    }

    @Test
    public void clipsNestedContentToAllAncestors() {
        Map<String, AnchorLayout.Bounds> bounds = new LinkedHashMap<>();
        bounds.put("screen", new AnchorLayout.Bounds(0, 0, 300, 200));
        bounds.put("demo:root/__content", new AnchorLayout.Bounds(20, 20, 200, 100));
        bounds.put("demo:child/__content", new AnchorLayout.Bounds(180, 40, 90, 120));
        Map<String, String> parents = Map.of("demo:child", "demo:root", "demo:leaf", "demo:child");
        var clips = WidgetContainers.clips(bounds, parents, java.util.Set.of("demo:root", "demo:child"));
        assertEquals(new AnchorLayout.Bounds(20, 20, 200, 100), clips.get("demo:child"));
        assertEquals(new AnchorLayout.Bounds(180, 40, 40, 80), clips.get("demo:leaf"));
        assertTrue(WidgetContainers.contains(clips.get("demo:leaf"), 190, 50));
        assertFalse(WidgetContainers.contains(clips.get("demo:leaf"), 240, 50));
    }
}
