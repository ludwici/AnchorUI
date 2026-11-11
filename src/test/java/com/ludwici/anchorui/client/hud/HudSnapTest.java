package com.ludwici.anchorui.client.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HudSnapTest {
    private static HudSnap.Widget widget(String id, String parent, int x, int y, int width, int height) {
        return new HudSnap.Widget(id, parent, x, y, width, height, true);
    }

    @Test
    public void alignsLeftAndTopCoordinatesOnSeparateAxes() {
        var moving = widget("mod:moving", "screen", 15, 20, 40, 18);
        var other = widget("mod:other", "screen", 100, 500, 80, 20);

        var result = HudSnap.snap(moving, 96, 496, List.of(moving, other));
        assertEquals(100, result.x());
        assertEquals(500, result.y());
        assertEquals(100, result.guideX().intValue());
        assertEquals(500, result.guideY().intValue());
    }

    @Test
    public void alignsCentersAndRightEdges() {
        var moving = widget("mod:moving", "screen", 0, 0, 40, 16);
        var target = widget("mod:target", "screen", 100, 200, 80, 18);

        var center = HudSnap.snap(moving, 117, 100, List.of(moving, target));
        assertEquals(120, center.x());
        assertEquals(140, center.guideX().intValue());

        var right = HudSnap.snap(moving, 141, 100, List.of(moving, target));
        assertEquals(140, right.x());
        assertEquals(180, right.guideX().intValue());
    }

    @Test
    public void snapsToAdjacentEdgesWithoutChangingTheOtherAxis() {
        var moving = widget("mod:moving", "screen", 0, 0, 40, 16);
        var target = widget("mod:target", "screen", 100, 200, 50, 20);

        var result = HudSnap.snap(moving, 147, 310, List.of(moving, target));
        assertEquals(150, result.x());
        assertEquals(310, result.y());
        assertEquals(150, result.guideX().intValue());
        assertNull(result.guideY());
    }

    @Test
    public void doesNotSnapBeyondSixGuiPixels() {
        var moving = widget("mod:moving", "screen", 0, 0, 40, 20);
        var target = widget("mod:target", "screen", 100, 200, 80, 20);

        var result = HudSnap.snap(moving, 90, 310, List.of(moving, target));
        assertEquals(90, result.x());
        assertEquals(310, result.y());
        assertNull(result.guideX());
        assertNull(result.guideY());
    }

    @Test
    public void ignoresHiddenWidgetsAndMovingDescendants() {
        var moving = widget("mod:parent", "screen", 20, 20, 40, 18);
        var child = widget("mod:child", "mod:parent", 100, 500, 40, 20);
        var grandchild = widget("mod:grandchild", "mod:child", 200, 600, 40, 20);
        var hidden = new HudSnap.Widget("mod:hidden", "screen", 300, 700, 40, 20, false);

        var result = HudSnap.snap(moving, 103, 503, List.of(moving, child, grandchild, hidden));
        assertEquals(103, result.x());
        assertEquals(503, result.y());
        assertNull(result.guideX());
        assertNull(result.guideY());
    }

    @Test
    public void snapResultIsBasedOnRawMousePositionNotPreviousSnap() {
        var moving = widget("mod:moving", "screen", 10, 10, 40, 20);
        var target = widget("mod:target", "screen", 100, 200, 80, 20);

        assertEquals(100, HudSnap.snap(moving, 96, 300, List.of(moving, target)).x());
        assertEquals(110, HudSnap.snap(moving, 110, 300, List.of(moving, target)).x());
    }
}
