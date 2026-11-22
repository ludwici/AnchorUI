package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.layout.AnchorLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NativeContainerScrollPanelTest {
    @Test
    public void logicalOffsetsStayWithinScrollLimits() {
        NativeContainerScrollPanel panel = new NativeContainerScrollPanel(null, new AnchorLayout.Bounds(10, 20, 100, 50), 100, 8, 1.0, 0);
        assertEquals(0, panel.logicalOffset());
        assertTrue(panel.mouseScrolled(40, 30, 0, -2));
        assertEquals(16, panel.logicalOffset());
        assertFalse(panel.mouseScrolled(0, 0, 0, -1));
        panel.setLogicalOffset(200);
        assertEquals(100, panel.logicalOffset());
        panel.setLogicalOffset(-100);
        assertEquals(0, panel.logicalOffset());
    }

    @Test
    public void draggingScrollbarUpdatesOffsetWithoutCapturingContentClicks() {
        NativeContainerScrollPanel panel = new NativeContainerScrollPanel(null, new AnchorLayout.Bounds(10, 20, 100, 50), 100, 8, 1.0, 0);
        assertFalse(panel.mouseClicked(20, 30, 0));
        assertTrue(panel.mouseClicked(108, 25, 0));
        assertTrue(panel.isDraggingBar());
        assertTrue(panel.mouseDragged(108, 45, 0, 0, 20));
        assertTrue(panel.logicalOffset() > 0);
        assertTrue(panel.mouseReleased(108, 45, 0));
        assertFalse(panel.isDraggingBar());
        assertFalse(panel.mouseReleased(108, 45, 0));
    }
}
