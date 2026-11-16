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
        assertTrue(sections.expanded("anchors"));
        assertFalse(sections.expanded("constraint:demo:bar:0"));
        assertFalse(sections.expanded("animation"));
        assertFalse(sections.expanded("source"));
        assertFalse(sections.expanded("diagnostics"));
        assertEquals("DIAGNOSTICS (1 error, 2 warnings) [+]", sections.label("diagnostics", "DIAGNOSTICS (1 error, 2 warnings)"));
        assertEquals("ANIMATION [+]", sections.label("animation", "ANIMATION"));
        sections.toggle("constraint:demo:bar:0");
        assertTrue(sections.expanded("constraint:demo:bar:0"));
        assertFalse(sections.expanded("constraint:demo:bar:1"));
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
    public void headerLookupSelectsScrolledAnchorRowsWithoutTogglingState() {
        InspectorSections sections = new InspectorSections();
        sections.register("anchors", 0);
        sections.register("constraint:demo:bar:0", 3);
        sections.register("constraint:demo:bar:1", 11);
        assertEquals("constraint:demo:bar:1", sections.headerAt(107, 100, 200, 136, 13));
        assertFalse(sections.expanded("constraint:demo:bar:1"));
        assertTrue(sections.click(107, 100, 200, 136, 13));
        assertTrue(sections.expanded("constraint:demo:bar:1"));
        assertEquals(null, sections.headerAt(200, 100, 200, 0, 13));
        assertEquals(null, sections.headerAt(90, 100, 200, 0, 13));
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
