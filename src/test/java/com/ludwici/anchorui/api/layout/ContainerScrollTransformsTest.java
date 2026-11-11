package com.ludwici.anchorui.api.layout;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ContainerScrollTransformsTest {
    @Test
    public void scrollingMovesChildrenButKeepsPanelAndViewportInPlace() {
        Map<String, AnchorLayout.Bounds> bounds = Map.of(
                "screen", new AnchorLayout.Bounds(0, 0, 400, 300),
                "demo:panel", new AnchorLayout.Bounds(50, 30, 130, 80),
                "demo:panel/__content", new AnchorLayout.Bounds(58, 38, 114, 64),
                "demo:child", new AnchorLayout.Bounds(60, 40, 100, 200)
        );
        Map<String, String> parents = Map.of("demo:child", "demo:panel");
        Map<String, WidgetTransforms.Group> groups = Map.of(
                "demo:panel", new WidgetTransforms.Group(0.5, true));

        var original = WidgetTransforms.resolve(bounds, parents, groups);
        var scrolled = WidgetTransforms.resolve(bounds, parents, groups,
                Map.of("demo:panel", new ContainerScroll.Offset(0, 40)));

        assertEquals(original.get("demo:panel"), scrolled.get("demo:panel"));
        assertEquals(original.get("demo:child").scale(), scrolled.get("demo:child").scale());
        assertEquals(original.get("demo:child").translateX(), scrolled.get("demo:child").translateX());
        assertEquals(original.get("demo:child").translateY() - 20.0, scrolled.get("demo:child").translateY());
        assertNotEquals(original.get("demo:child").physical(bounds.get("demo:child")),
                scrolled.get("demo:child").physical(bounds.get("demo:child")));

        var viewport = WidgetContainers.clips(bounds, parents, Set.of("demo:panel"), scrolled);
        assertEquals(new AnchorLayout.Bounds(86, 54, 56, 32), viewport.get("demo:child"));
    }

    @Test
    public void independentNestedOffsetsAccumulateThroughTransforms() {
        Map<String, AnchorLayout.Bounds> bounds = Map.of(
                "screen", new AnchorLayout.Bounds(0, 0, 500, 400),
                "demo:outer", new AnchorLayout.Bounds(20, 20, 160, 130),
                "demo:outer/__content", new AnchorLayout.Bounds(24, 24, 152, 122),
                "demo:inner", new AnchorLayout.Bounds(30, 40, 100, 80),
                "demo:inner/__content", new AnchorLayout.Bounds(34, 44, 92, 72),
                "demo:leaf", new AnchorLayout.Bounds(40, 60, 80, 30)
        );
        Map<String, String> parents = Map.of("demo:inner", "demo:outer", "demo:leaf", "demo:inner");
        Map<String, ContainerScroll.Offset> offsets = Map.of(
                "demo:outer", new ContainerScroll.Offset(0, 15),
                "demo:inner", new ContainerScroll.Offset(0, 7)
        );
        var transforms = WidgetTransforms.resolve(bounds, parents, Map.of(), offsets);

        assertEquals(20, transforms.get("demo:outer").y(20));
        assertEquals(25, transforms.get("demo:inner").y(40));
        assertEquals(38, transforms.get("demo:leaf").y(60));
    }
}
