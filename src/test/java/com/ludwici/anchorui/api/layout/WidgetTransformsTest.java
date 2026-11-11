package com.ludwici.anchorui.api.layout;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WidgetTransformsTest {
    @Test
    public void scalesACompletePanelTreeAroundTopLeft() {
        var bounds = Map.of(
                "screen", new AnchorLayout.Bounds(0, 0, 500, 300),
                "demo:parent", new AnchorLayout.Bounds(100, 50, 200, 150),
                "demo:parent/__content", new AnchorLayout.Bounds(110, 60, 180, 130),
                "demo:child", new AnchorLayout.Bounds(110, 60, 30, 20));
        var transforms = WidgetTransforms.resolve(bounds, Map.of("demo:child", "demo:parent"),
                Map.of("demo:parent", new WidgetTransforms.Group(0.5, false)));
        assertEquals(new AnchorLayout.Bounds(100, 50, 100, 75), transforms.get("demo:parent").physical(bounds.get("demo:parent")));
        assertEquals(new AnchorLayout.Bounds(105, 55, 15, 10), transforms.get("demo:child").physical(bounds.get("demo:child")));
        assertEquals(110.0, transforms.get("demo:child").inverseX(105), 0.00001);
    }

    @Test
    public void centerScalePreservesThePanelCenter() {
        var bounds = Map.of("screen", new AnchorLayout.Bounds(0, 0, 500, 300),
                "demo:parent", new AnchorLayout.Bounds(150, 60, 200, 100));
        var t = WidgetTransforms.resolve(bounds, Map.of(),
                Map.of("demo:parent", new WidgetTransforms.Group(0.5, true))).get("demo:parent");
        assertEquals(new AnchorLayout.Bounds(200, 85, 100, 50), t.physical(bounds.get("demo:parent")));
        assertEquals(250.0, t.x(250), 0.00001);
    }

    @Test
    public void nestedScalesMultiplyAndUnrelatedWidgetsRemainUnchanged() {
        var bounds = Map.of("screen", new AnchorLayout.Bounds(0, 0, 500, 300),
                "demo:parent", new AnchorLayout.Bounds(40, 60, 200, 100),
                "demo:inner", new AnchorLayout.Bounds(60, 70, 80, 40),
                "demo:leaf", new AnchorLayout.Bounds(80, 85, 20, 10),
                "demo:other", new AnchorLayout.Bounds(10, 20, 50, 10));
        var transforms = WidgetTransforms.resolve(bounds, Map.of("demo:inner", "demo:parent", "demo:leaf", "demo:inner"),
                Map.of("demo:parent", new WidgetTransforms.Group(0.5, false),
                        "demo:inner", new WidgetTransforms.Group(0.5, false)));
        assertEquals(0.25, transforms.get("demo:leaf").scale(), 0.00001);
        assertEquals(new AnchorLayout.Bounds(10, 20, 50, 10), transforms.get("demo:other").physical(bounds.get("demo:other")));
    }

    @Test
    public void rejectsInvalidGroupScales() {
        assertThrows(IllegalArgumentException.class, () -> new WidgetTransforms.Group(0.0, false));
        assertThrows(IllegalArgumentException.class, () -> new WidgetTransforms.Group(Double.NaN, false));
    }
}
