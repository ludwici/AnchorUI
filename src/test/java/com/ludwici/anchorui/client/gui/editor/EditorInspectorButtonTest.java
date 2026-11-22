package com.ludwici.anchorui.client.gui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorInspectorButtonTest {
    @Test
    public void controlsCenterInsideThirteenPixelInspectorRows() {
        assertEquals(11, EditorInspectorButton.centeredInRow(10, 13));
        assertEquals(10, EditorInspectorButton.centeredInRow(10, 11));
        assertEquals(12, EditorInspectorButton.centeredInRow(10, 15));
    }

    @Test
    public void textCentersAccordingToScaledFontMetrics() {
        float expected = (13.0F - 8.0F * 0.83F) / 2.0F;
        assertEquals(expected, EditorInspectorButton.centeredTextInRow(0, 13, 9, 0.83F), 0.001F);
        assertEquals(expected + 10.0F, EditorInspectorButton.centeredTextInRow(10, 13, 9, 0.83F), 0.001F);
    }

    @Test
    public void visualButtonsUseSquareDimensionsWithIndependentClickableRows() {
        assertEquals(11, EditorInspectorButton.SIZE);
        assertTrue(EditorInspectorButton.contains(15, 30, 10, 30));
        assertTrue(EditorInspectorButton.contains(23, 40, 10, 30));
        assertFalse(EditorInspectorButton.contains(24, 30, 10, 30));
        assertFalse(EditorInspectorButton.contains(15, 41, 10, 30));
    }
}
