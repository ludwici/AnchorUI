package com.ludwici.anchorui.client.gui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorInspectorButtonTest {
    @Test
    public void visualButtonsUseSquareDimensionsWithIndependentClickableRows() {
        assertEquals(11, EditorInspectorButton.SIZE);
        assertTrue(EditorInspectorButton.contains(15, 30, 10, 30));
        assertTrue(EditorInspectorButton.contains(23, 40, 10, 30));
        assertFalse(EditorInspectorButton.contains(24, 30, 10, 30));
        assertFalse(EditorInspectorButton.contains(15, 41, 10, 30));
    }
}
