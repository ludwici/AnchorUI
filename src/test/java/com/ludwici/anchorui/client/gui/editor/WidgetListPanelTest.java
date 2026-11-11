package com.ludwici.anchorui.client.gui.editor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetListPanelTest {
    @Test
    public void listsStartCollapsedAndKeepIndependentExpansionState() {
        WidgetListPanel first = new WidgetListPanel("anchorui");
        WidgetListPanel second = new WidgetListPanel("catelot");
        first.resize(450, 300);
        second.resize(450, 300);

        assertTrue(first.isCollapsed());
        assertTrue(second.isCollapsed());
        assertTrue(first.beginClick(90, 15, 0, List.of()));
        assertFalse(first.isCollapsed());
        assertTrue(second.isCollapsed());

        first.setCollapsed(true);
        assertTrue(first.isCollapsed());
        assertTrue(second.isCollapsed());
    }

    @Test
    public void collapsingCategoriesKeepsRowsAndSelectionDataIntact() {
        WidgetListPanel list = new WidgetListPanel("anchorui");
        list.resize(450, 300);
        var entries = List.of(
                new WidgetListPanel.Entry("minecraft:hotbar", true, true),
                new WidgetListPanel.Entry("anchorui:status", true, false));

        list.setCollapsed(false);
        assertFalse(list.isGroupCollapsed(0));
        assertTrue(list.beginClick(20, 37, 0, entries));
        assertTrue(list.isGroupCollapsed(0));
        assertTrue(list.beginClick(20, 37, 0, entries));
        assertFalse(list.isGroupCollapsed(0));
        assertNull(list.entryAt(20, 36, entries));
    }

    @Test
    public void collapsingDoesNotLetHiddenListCaptureClicksBelowHeader() {
        WidgetListPanel list = new WidgetListPanel("anchorui");
        list.resize(450, 300);
        assertFalse(list.contains(30, 40));
        list.setCollapsed(false);
        assertTrue(list.contains(30, 40));
        list.setCollapsed(true);
        assertFalse(list.contains(30, 40));
    }
}
