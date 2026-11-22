package com.ludwici.anchorui.client.gui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorInspectorTabsTest {
    @Test
    public void pagesAreIndependentAndClickable() {
        EditorInspectorTabs tabs = new EditorInspectorTabs();
        assertTrue(tabs.active(EditorInspectorTabs.Page.POSITION));
        assertFalse(tabs.click(12, 60, 10, 40, 210));
        assertEquals(EditorInspectorTabs.Page.POSITION, tabs.selected());
        assertTrue(tabs.click(75, 45, 10, 40, 210));
        assertEquals(EditorInspectorTabs.Page.LAYOUT, tabs.selected());
        assertTrue(tabs.click(195, 46, 10, 40, 210));
        assertEquals(EditorInspectorTabs.Page.MORE, tabs.selected());
        assertFalse(tabs.active(EditorInspectorTabs.Page.MOTION));
        tabs.select(EditorInspectorTabs.Page.STYLE);
        assertEquals(EditorInspectorTabs.Page.STYLE, tabs.selected());
    }

    @Test
    public void spacersAndOutsideClicksDoNotChangeTab() {
        EditorInspectorTabs tabs = new EditorInspectorTabs();
        assertFalse(tabs.click(51, 44, 10, 40, 210));
        assertFalse(tabs.click(10, 39, 10, 40, 210));
        assertFalse(tabs.click(221, 43, 10, 40, 210));
        assertTrue(tabs.active(EditorInspectorTabs.Page.POSITION));
    }
}
