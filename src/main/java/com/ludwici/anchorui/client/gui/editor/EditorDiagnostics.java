package com.ludwici.anchorui.client.gui.editor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Creates a bounded, deduplicated view of diagnostics for both F8 inspectors.
 * A full copy of each warning remains in the Minecraft log.
 */
public final class EditorDiagnostics {
    private static final int MAX_VISIBLE = 24;

    public enum Severity {
        ERROR, WARNING
    }

    public record Entry(Severity severity, String message) {
    }

    public record Summary(List<Entry> entries, int errors, int warnings, int omitted) {
        public boolean empty() {
            return errors == 0 && warnings == 0;
        }

        public String title() {
            return "DIAGNOSTICS (" + errors + (errors == 1 ? " error, " : " errors, ") + warnings + (warnings == 1 ? " warning)" : " warnings)");
        }
    }

    private EditorDiagnostics() {
    }

    public static Summary summarize(String error, List<String> warnings) {
        List<Entry> entries = new ArrayList<>();
        Set<String> uniqueWarnings = new LinkedHashSet<>();
        String normalizedError = error == null ? "" : error.trim();
        if (!normalizedError.isEmpty()) {
            entries.add(new Entry(Severity.ERROR, normalizedError));
        }
        if (warnings != null) {
            for (String warning : warnings) {
                if (warning != null && !warning.isBlank()) {
                    uniqueWarnings.add(warning.trim());
                }
            }
        }
        for (String warning : uniqueWarnings) {
            if (entries.size() >= MAX_VISIBLE) {
                break;
            }
            entries.add(new Entry(Severity.WARNING, warning));
        }
        int count = uniqueWarnings.size() + (normalizedError.isEmpty() ? 0 : 1);
        return new Summary(List.copyOf(entries), normalizedError.isEmpty() ? 0 : 1, uniqueWarnings.size(), Math.max(0, count - entries.size()));
    }
}
