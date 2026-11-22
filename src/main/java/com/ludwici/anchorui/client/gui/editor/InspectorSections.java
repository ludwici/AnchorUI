package com.ludwici.anchorui.client.gui.editor;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InspectorSections {
    private final Map<String, Boolean> expanded = new LinkedHashMap<>();
    private final Map<String, Integer> headers = new LinkedHashMap<>();
    private final java.util.Set<Integer> headerRows = new java.util.HashSet<>();

    public void clearHeaders() {
        headers.clear();
        headerRows.clear();
    }

    public void register(String id, int row) {
        headers.put(id, row);
        headerRows.add(row);
    }

    public boolean isHeaderRow(int row) {
        return headerRows.contains(row);
    }

    public boolean expanded(String id) {
        return expanded.getOrDefault(id, !id.equals("source") && !id.equals("diagnostics") && !id.startsWith("track:") && !id.startsWith("constraint:"));
    }

    public void collapse(String id) {
        expanded.put(id, false);
    }

    public String label(String id, String title) {
        return title + (expanded(id) ? " [-]" : " [+]");
    }

    public void toggle(String id) {
        expanded.put(id, !expanded(id));
    }

    public String headerAt(double mouseY, int top, int bottom, int scroll, int rowHeight) {
        for (var entry : headers.entrySet()) {
            int rowY = top - scroll + entry.getValue() * rowHeight;
            if (rowY >= top && rowY + rowHeight <= bottom && mouseY >= rowY && mouseY < rowY + rowHeight) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean click(double mouseY, int top, int bottom, int scroll, int rowHeight) {
        String id = headerAt(mouseY, top, bottom, scroll, rowHeight);
        if (id == null) {
            return false;
        }
        toggle(id);
        return true;
    }
}
