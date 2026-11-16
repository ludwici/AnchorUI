package com.ludwici.anchorui.client.gui.editor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorDiagnosticsTest {
    @Test
    public void removesEmptyMessagesAndDuplicatesButKeepsIssueOrder() {
        EditorDiagnostics.Summary summary = EditorDiagnostics.summarize(" Layout failed ", List.of("Missing widget type", " Missing widget type ", "", "Bad condition"));
        assertEquals(1, summary.errors());
        assertEquals(2, summary.warnings());
        assertEquals(0, summary.omitted());
        assertEquals(3, summary.entries().size());
        assertEquals("Layout failed", summary.entries().get(0).message());
        assertEquals(EditorDiagnostics.Severity.ERROR, summary.entries().get(0).severity());
        assertEquals("Missing widget type", summary.entries().get(1).message());
        assertEquals("Bad condition", summary.entries().get(2).message());
        assertEquals("DIAGNOSTICS (1 error, 2 warnings)", summary.title());
    }

    @Test
    public void emptyDiagnosticsNeedNoSection() {
        assertTrue(EditorDiagnostics.summarize(null, null).empty());
        assertTrue(EditorDiagnostics.summarize(" ", List.of(" ", "")).empty());
        assertFalse(EditorDiagnostics.summarize(null, List.of("missing texture")).empty());
        assertEquals("DIAGNOSTICS (0 errors, 1 warning)", EditorDiagnostics.summarize(null, List.of("missing texture")).title());
    }

    @Test
    public void largeWarningSetsAreBoundedWithoutLosingTheActualCount() {
        List<String> warnings = new ArrayList<>();
        for (int index = 0; index < 40; index++) {
            warnings.add("Widget " + index);
        }
        EditorDiagnostics.Summary summary = EditorDiagnostics.summarize("One fatal error", warnings);
        assertEquals(1, summary.errors());
        assertEquals(40, summary.warnings());
        assertEquals(24, summary.entries().size());
        assertEquals(17, summary.omitted());
        assertEquals("Widget 22", summary.entries().getLast().message());
        assertEquals("DIAGNOSTICS (1 error, 40 warnings)", summary.title());
    }
}
