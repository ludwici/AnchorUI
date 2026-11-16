package com.ludwici.anchorui.client.gui.editor;

import com.ludwici.anchorui.client.LoreResourceCompiler;

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
        return summarize(error, warnings, List.of());
    }

    public static Summary summarize(String error, List<String> warnings, List<LoreResourceCompiler.Issue> resources) {
        Set<Entry> unique = new LinkedHashSet<>();
        if (error != null && !error.isBlank()) {
            unique.add(new Entry(Severity.ERROR, error.trim()));
        }
        for (LoreResourceCompiler.Issue issue : resources) {
            if (issue.severity() == LoreResourceCompiler.Severity.ERROR) {
                unique.add(new Entry(Severity.ERROR, issue.display()));
            }
        }
        if (warnings != null) {
            for (String warning : warnings) {
                if (warning != null && !warning.isBlank()) {
                    unique.add(new Entry(Severity.WARNING, warning.trim()));
                }
            }
        }
        for (LoreResourceCompiler.Issue issue : resources) {
            if (issue.severity() == LoreResourceCompiler.Severity.WARNING) {
                unique.add(new Entry(Severity.WARNING, issue.display()));
            }
        }
        List<Entry> entries = new ArrayList<>();
        int errors = 0;
        int warningCount = 0;
        for (Entry entry : unique) {
            if (entry.severity() == Severity.ERROR) {
                errors++;
            } else {
                warningCount++;
            }
            if (entries.size() < MAX_VISIBLE) {
                entries.add(entry);
            }
        }
        return new Summary(List.copyOf(entries), errors, warningCount, Math.max(0, unique.size() - entries.size()));
    }
}
