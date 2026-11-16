package com.ludwici.anchorui.client.gui.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InspectorSectionsTest {
    @Test
    public void sourceAnimationAndDiagnosticsStartCollapsedButOtherSectionsStartExpanded() {
        InspectorSections sections = new InspectorSections();
        assertTrue(sections.expanded("widget"));
        assertTrue(sections.expanded("layout"));
        assertTrue(sections.expanded("visual"));
        assertFalse(sections.expanded("animation"));
        assertFalse(sections.expanded("source"));
        assertFalse(sections.expanded("diagnostics"));
        assertEquals("DIAGNOSTICS (1 error, 2 warnings) [+]", sections.label("diagnostics", "DIAGNOSTICS (1 error, 2 warnings)"));
        assertEquals("ANIMATION [+]", sections.label("animation", "ANIMATION"));
        sections.toggle("animation");
        assertTrue(sections.expanded("animation"));
        assertEquals("ANIMATION [-]", sections.label("animation", "ANIMATION"));
    }

    @Test
    public void clickChecksVisibleRowPositionAfterScrollAndSurvivesHeaderRebuild() {
        InspectorSections sections = new InspectorSections();
        sections.register("widget", 0);
        sections.register("layout", 10);
        sections.register("visual", 20);
        assertFalse(sections.click(114, 100, 180, 0, 13));
        assertTrue(sections.expanded("layout"));
        assertTrue(sections.click(106, 100, 180, 130, 13));
        assertFalse(sections.expanded("layout"));
        sections.clearHeaders();
        sections.register("layout", 0);
        assertTrue(sections.click(100, 100, 180, 0, 13));
        assertTrue(sections.expanded("layout"));
        assertFalse(sections.click(100, 100, 180, 30, 13));
        assertTrue(sections.expanded("layout"));
    }

    @Test
    public void sourceCollapseIsIndependentFromOtherCategories() {
        InspectorSections sections = new InspectorSections();
        sections.toggle("source");
        sections.toggle("animation");
        sections.collapse("source");
        assertFalse(sections.expanded("source"));
        assertTrue(sections.expanded("animation"));
    }
}
