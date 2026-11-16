package com.ludwici.anchorui.client.gui.editor;

import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AnchorInspectorControlsTest {
    private static final PositionElement.Constraint BASE = new PositionElement.Constraint(ElementAnchor.LEFT, ElementAnchor.RIGHT, "demo:panel", 8, -2);

    @Test
    public void newAnchorCopiesAnExistingConstraintWithoutChangingLayoutUntilEdited() {
        List<PositionElement.Constraint> original = List.of(BASE);
        List<PositionElement.Constraint> added = AnchorInspectorControls.add(original);
        assertEquals(2, added.size());
        assertEquals(BASE, added.getFirst());
        assertEquals(BASE, added.get(1));
        assertEquals(1, original.size());

        List<PositionElement.Constraint> changed = AnchorInspectorControls.change(added, 1, "point", "RIGHT");
        changed = AnchorInspectorControls.change(changed, 1, "anchor", "LEFT");
        changed = AnchorInspectorControls.change(changed, 1, "relativeTo", "demo:target");
        assertEquals(ElementAnchor.RIGHT, changed.get(1).point());
        assertEquals(ElementAnchor.LEFT, changed.get(1).anchor());
        assertEquals("demo:target", changed.get(1).relativeTo());
        assertEquals(BASE, original.getFirst());
        assertEquals(BASE, changed.getFirst());
    }

    @Test
    public void editsAndRemovesIndividualOffsetsWithoutTouchingOtherAnchors() {
        List<PositionElement.Constraint> anchors = AnchorInspectorControls.add(List.of(BASE));
        anchors = AnchorInspectorControls.change(anchors, 1, "x", "-15");
        anchors = AnchorInspectorControls.step(anchors, 1, "y", 10);
        assertEquals(-15, anchors.get(1).x());
        assertEquals(8, anchors.get(1).y());
        assertEquals(BASE, anchors.getFirst());

        List<PositionElement.Constraint> remaining = AnchorInspectorControls.remove(anchors, 0);
        assertEquals(1, remaining.size());
        assertEquals(-15, remaining.getFirst().x());
        assertEquals(8, remaining.getFirst().y());
        assertEquals(BASE, AnchorInspectorControls.remove(anchors, 1).getFirst());
    }

    @Test
    public void cannotDeleteFinalAnchorOrChangeInvalidFields() {
        List<PositionElement.Constraint> anchors = List.of(BASE);
        assertThrows(IllegalArgumentException.class, () -> AnchorInspectorControls.remove(anchors, 0));
        assertThrows(IllegalArgumentException.class, () -> AnchorInspectorControls.change(anchors, 1, "point", "LEFT"));
        assertThrows(IllegalArgumentException.class, () -> AnchorInspectorControls.change(anchors, 0, "point", "WRONG"));
        assertThrows(IllegalArgumentException.class, () -> AnchorInspectorControls.change(anchors, 0, "x", "not a number"));
        assertThrows(ArithmeticException.class, () -> AnchorInspectorControls.step(anchors, 0, "x", Integer.MAX_VALUE));
    }
}
