package com.ludwici.anchorui.client.gui.editor;

import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EditorAnchorGuideTest {
    @Test
    public void resolvesAllNinePointsWithSameIntegerRoundingAsLayout() {
        AnchorLayout.Bounds bounds = new AnchorLayout.Bounds(21, 43, 101, 31);
        assertEquals(new EditorAnchorGuide.Point(21, 43), EditorAnchorGuide.point(bounds, ElementAnchor.TOPLEFT));
        assertEquals(new EditorAnchorGuide.Point(71, 43), EditorAnchorGuide.point(bounds, ElementAnchor.TOP));
        assertEquals(new EditorAnchorGuide.Point(122, 43), EditorAnchorGuide.point(bounds, ElementAnchor.TOPRIGHT));
        assertEquals(new EditorAnchorGuide.Point(21, 58), EditorAnchorGuide.point(bounds, ElementAnchor.LEFT));
        assertEquals(new EditorAnchorGuide.Point(71, 58), EditorAnchorGuide.point(bounds, ElementAnchor.CENTER));
        assertEquals(new EditorAnchorGuide.Point(122, 58), EditorAnchorGuide.point(bounds, ElementAnchor.RIGHT));
        assertEquals(new EditorAnchorGuide.Point(21, 74), EditorAnchorGuide.point(bounds, ElementAnchor.BOTTOMLEFT));
        assertEquals(new EditorAnchorGuide.Point(71, 74), EditorAnchorGuide.point(bounds, ElementAnchor.BOTTOM));
        assertEquals(new EditorAnchorGuide.Point(122, 74), EditorAnchorGuide.point(bounds, ElementAnchor.BOTTOMRIGHT));
    }

    @Test
    public void connectsSourcePointToTargetWithIndependentOffsets() {
        AnchorLayout.Bounds widget = new AnchorLayout.Bounds(75, 60, 180, 10);
        AnchorLayout.Bounds target = new AnchorLayout.Bounds(10, 42, 40, 30);
        PositionElement.Constraint anchor = new PositionElement.Constraint(ElementAnchor.LEFT, ElementAnchor.RIGHT, "example:left", 12, -2);
        EditorAnchorGuide.Connection guide = EditorAnchorGuide.connect(widget, target, anchor);
        assertEquals(new EditorAnchorGuide.Point(75, 65), guide.widget());
        assertEquals(new EditorAnchorGuide.Point(50, 57), guide.target());
        assertEquals(new EditorAnchorGuide.Point(62, 55), guide.offsetTarget());
        assertEquals(12, guide.offsetX());
        assertEquals(-2, guide.offsetY());
    }

    @Test
    public void zeroOffsetsOrVanillaBoundsUseSameGeometry() {
        AnchorLayout.Bounds widget = new AnchorLayout.Bounds(100, 52, 82, 18);
        AnchorLayout.Bounds vanillaHotbar = new AnchorLayout.Bounds(91, 200, 182, 22);
        PositionElement.Constraint anchor = new PositionElement.Constraint(ElementAnchor.TOP, ElementAnchor.TOP, "minecraft:hotbar", 0, 0);
        EditorAnchorGuide.Connection guide = EditorAnchorGuide.connect(widget, vanillaHotbar, anchor);
        assertEquals(new EditorAnchorGuide.Point(141, 52), guide.widget());
        assertEquals(new EditorAnchorGuide.Point(182, 200), guide.target());
        assertEquals(guide.target(), guide.offsetTarget());
    }
}
