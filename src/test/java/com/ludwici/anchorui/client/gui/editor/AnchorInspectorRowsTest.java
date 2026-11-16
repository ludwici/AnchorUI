package com.ludwici.anchorui.client.gui.editor;

import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnchorInspectorRowsTest {
    @Test
    public void listsAllEffectiveAnchorsAndLabelsThePrimary() {
        PositionElement source = new PositionElement(List.of(
                new PositionElement.Constraint(ElementAnchor.LEFT, ElementAnchor.RIGHT, "demo:left_card", 8, 0),
                new PositionElement.Constraint(ElementAnchor.RIGHT, ElementAnchor.LEFT, "demo:right_card", -8, 0)));
        PositionElement adjusted = source.withPrimary(ElementAnchor.LEFT, ElementAnchor.RIGHT, "demo:changed").translated(3, -2);

        assertEquals(List.of(
                "#1 (primary): LEFT -> RIGHT",
                "  relativeTo: demo:changed",
                "  offset: 11, -2",
                "#2: RIGHT -> LEFT",
                "  relativeTo: demo:right_card",
                "  offset: -5, -2"), AnchorInspectorRows.describe(adjusted.anchors));
        assertEquals("size axes: width anchored, height fixed", AnchorInspectorRows.dimensions(adjusted.anchors));
        assertEquals("demo:left_card", source.anchors.getFirst().relativeTo());
        assertEquals(8, source.anchors.getFirst().x());
    }

    @Test
    public void distinguishesSingleAndVerticalAnchorsAndEmptyResults() {
        PositionElement one = new PositionElement(ElementAnchor.CENTER, ElementAnchor.CENTER, "screen", 0, 12);
        assertEquals(3, AnchorInspectorRows.describe(one.anchors).size());
        assertEquals("size axes: width fixed, height fixed", AnchorInspectorRows.dimensions(one.anchors));

        PositionElement height = new PositionElement(List.of(
                new PositionElement.Constraint(ElementAnchor.TOP, ElementAnchor.TOP, "screen", 0, 6),
                new PositionElement.Constraint(ElementAnchor.BOTTOM, ElementAnchor.BOTTOM, "screen", 0, -6)));
        assertEquals("size axes: width fixed, height anchored", AnchorInspectorRows.dimensions(height.anchors));
        assertTrue(AnchorInspectorRows.describe(List.of()).isEmpty());
        assertEquals("", AnchorInspectorRows.dimensions(List.of()));
    }
}
