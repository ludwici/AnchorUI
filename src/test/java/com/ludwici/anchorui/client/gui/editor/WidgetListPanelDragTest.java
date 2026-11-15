package com.ludwici.anchorui.client.gui.editor;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetListPanelDragTest {
    @Test
    public void draggingWidgetListDownShrinksItAndDraggingBackUpRestoresHeight() {
        WidgetListPanel panel = new WidgetListPanel("anchorui");
        panel.resize(600, 480);
        panel.setCollapsed(false);

        assertTrue(panel.contains(10, 365));
        assertTrue(panel.beginClick(10, 12, GLFW.GLFW_MOUSE_BUTTON_LEFT, List.of()));
        assertTrue(panel.dragged(10, 800, GLFW.GLFW_MOUSE_BUTTON_LEFT));
        assertTrue(panel.contains(10, 465));
        assertFalse(panel.contains(10, 473));

        assertTrue(panel.dragged(10, 12, GLFW.GLFW_MOUSE_BUTTON_LEFT));
        assertTrue(panel.contains(10, 365));
        assertFalse(panel.contains(10, 369));
        assertTrue(panel.released(GLFW.GLFW_MOUSE_BUTTON_LEFT));
    }

    @Test
    public void resizingScreenReclampsPositionAndExpandedHeight() {
        WidgetListPanel panel = new WidgetListPanel("anchorui");
        panel.resize(600, 480);
        panel.setCollapsed(false);
        assertTrue(panel.beginClick(10, 12, GLFW.GLFW_MOUSE_BUTTON_LEFT, List.of()));
        assertTrue(panel.dragged(10, 430, GLFW.GLFW_MOUSE_BUTTON_LEFT));
        assertTrue(panel.released(GLFW.GLFW_MOUSE_BUTTON_LEFT));

        panel.resize(600, 240);
        assertTrue(panel.contains(10, 180));
        assertFalse(panel.contains(10, 233));
    }
}
