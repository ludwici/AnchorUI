package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WidgetListPanel {
    public record Entry(String id, boolean visible, boolean readOnly) {
    }

    private record Row(Entry entry, int group, String header) {
        private boolean heading() {
            return entry == null;
        }
    }

    private static final int HEADER_HEIGHT = 21;
    private static final int FOOTER_HEIGHT = 17;
    private static final int ROW_HEIGHT = 17;
    private static final int GROUP_HEIGHT = 17;
    private static final int PANEL_BACKGROUND = 0xEE121D2B;
    private static final int HEADER_BACKGROUND = 0xEF29445B;
    private static final int SELECTED_COLOR = 0xFFFFD37A;
    private static final int VANILLA_COLOR = 0xFFB5A5EF;
    private static final int OWN_COLOR = 0xFF75DCDB;
    private static final int OTHER_COLOR = 0xFFD4E3F1;

    private final String currentNamespace;
    private int x = 8;
    private int y = 8;
    private int width = 214;
    private int height = 300;
    private int scroll;
    private int viewportWidth;
    private int viewportHeight;
    private int startX;
    private int startY;
    private double dragStartX;
    private double dragStartY;
    private boolean dragging;
    private List<Entry> cachedEntries = List.of();
    private List<Row> cachedRows = List.of();

    public WidgetListPanel(String currentNamespace) {
        this.currentNamespace = currentNamespace;
    }

    public void resize(int guiWidth, int guiHeight) {
        viewportWidth = guiWidth;
        viewportHeight = guiHeight;
        width = Math.min(224, Math.max(120, guiWidth / 3));
        height = Math.min(360, Math.max(64, guiHeight - 16));
        x = Math.max(0, Math.min(x, Math.max(0, guiWidth - width)));
        y = Math.max(0, Math.min(y, Math.max(0, guiHeight - height)));
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean beginClick(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && mouseY < y + HEADER_HEIGHT) {
            dragging = true;
            startX = x;
            startY = y;
            dragStartX = mouseX;
            dragStartY = mouseY;
        }
        return true;
    }

    public boolean dragged(double mouseX, double mouseY, int button) {
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        x = Math.max(0, Math.min(startX + (int) Math.round(mouseX - dragStartX), viewportWidth - width));
        y = Math.max(0, Math.min(startY + (int) Math.round(mouseY - dragStartY), viewportHeight - height));
        return true;
    }

    public boolean released(int button) {
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        dragging = false;
        return true;
    }

    public boolean scroll(double mouseX, double mouseY, double scrollY, List<Entry> entries) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        int max = maxScroll(rows(entries));
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.round(scrollY * ROW_HEIGHT * 2)));
        return true;
    }

    public Entry entryAt(double mouseX, double mouseY, List<Entry> entries) {
        if (!contains(mouseX, mouseY) || mouseY < y + HEADER_HEIGHT
                || mouseY >= y + height - FOOTER_HEIGHT) {
            return null;
        }
        int pointerY = (int) mouseY - y - HEADER_HEIGHT + scroll;
        int position = 0;
        for (Row row : rows(entries)) {
            int rowHeight = row.heading() ? GROUP_HEIGHT : ROW_HEIGHT;
            if (pointerY >= position && pointerY < position + rowHeight) {
                return row.entry();
            }
            position += rowHeight;
        }
        return null;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY,
                       List<Entry> entries, String selected, boolean choosingParent) {
        List<Row> rows = rows(entries);
        scroll = Math.min(scroll, maxScroll(rows));
        graphics.fill(x, y, x + width, y + height, PANEL_BACKGROUND);
        graphics.fill(x, y, x + width, y + HEADER_HEIGHT, HEADER_BACKGROUND);
        graphics.drawString(font, "Widgets (" + entries.size() + ")", x + 7, y + 7, 0xFFFFFFFF, false);

        int top = y + HEADER_HEIGHT;
        int bottom = y + height - FOOTER_HEIGHT;
        graphics.enableScissor(x + 2, top, x + width - 3, bottom);
        try {
            int lineY = top - scroll;
            for (Row row : rows) {
                int rowHeight = row.heading() ? GROUP_HEIGHT : ROW_HEIGHT;
                if (lineY + rowHeight > top && lineY < bottom) {
                    if (row.heading()) {
                        graphics.fill(x + 3, lineY, x + width - 4, lineY + rowHeight, 0xB1223244);
                        graphics.drawString(font, row.header(), x + 8, lineY + 4, color(row.group()), false);
                    } else {
                        Entry entry = row.entry();
                        boolean chosen = entry.id().equals(selected);
                        boolean hovering = mouseX >= x + 3 && mouseX < x + width - 4
                                && mouseY >= Math.max(top, lineY) && mouseY < Math.min(bottom, lineY + rowHeight);
                        if (chosen || hovering) {
                            graphics.fill(x + 3, lineY, x + width - 5, lineY + rowHeight,
                                    chosen ? 0xB04A4435 : 0x88405062);
                        }
                        int tint = chosen ? SELECTED_COLOR : entry.visible() ? color(row.group()) : 0xFF8993A1;
                        graphics.fill(x + 7, lineY + 5, x + 10, lineY + 12, tint);
                        String label = font.plainSubstrByWidth(entry.id(), width - 29);
                        graphics.drawString(font, label, x + 15, lineY + 4, tint, false);
                    }
                }
                lineY += rowHeight;
            }
        } finally {
            graphics.disableScissor();
        }
        graphics.fill(x + 3, bottom, x + width - 3, bottom + 1, 0xFF50647A);
        graphics.drawString(font, choosingParent ? "Click a parent ID" : "Click to select | Scroll",
                x + 6, bottom + 5, 0xFFB7C8D6, false);

        int totalHeight = contentHeight(rows);
        if (totalHeight > bottom - top) {
            int track = bottom - top;
            int thumb = Math.max(10, track * track / totalHeight);
            int thumbY = top + (track - thumb) * scroll / Math.max(1, totalHeight - track);
            graphics.fill(x + width - 5, top, x + width - 3, bottom, 0xFF374756);
            graphics.fill(x + width - 5, thumbY, x + width - 3, thumbY + thumb, 0xFF9FBDD0);
        }

        Entry hovered = entryAt(mouseX, mouseY, entries);
        if (hovered != null && font.width(hovered.id()) > width - 29) {
            graphics.renderTooltip(font, Component.literal(hovered.id()
                    + (hovered.readOnly() ? " (read-only)" : "")), mouseX, mouseY);
        }
    }

    private List<Row> rows(List<Entry> entries) {
        if (entries.equals(cachedEntries)) {
            return cachedRows;
        }
        Map<String, Entry> byId = new LinkedHashMap<>();
        for (Entry entry : entries) {
            byId.putIfAbsent(entry.id(), entry);
        }
        List<Row> result = new ArrayList<>();
        int previousGroup = -1;
        for (String id : EditorWidgetOrder.sorted(byId.keySet(), currentNamespace)) {
            int group = EditorWidgetOrder.group(id, currentNamespace);
            if (group != previousGroup) {
                String heading = switch (group) {
                    case 0 -> "VANILLA";
                    case 1 -> "CURRENT: " + currentNamespace;
                    default -> "OTHER MODS";
                };
                result.add(new Row(null, group, heading));
                previousGroup = group;
            }
            result.add(new Row(byId.get(id), group, null));
        }
        cachedEntries = List.copyOf(entries);
        cachedRows = List.copyOf(result);
        return cachedRows;
    }

    private int contentHeight(List<Row> rows) {
        int result = 0;
        for (Row row : rows) {
            result += row.heading() ? GROUP_HEIGHT : ROW_HEIGHT;
        }
        return result;
    }

    private int maxScroll(List<Row> rows) {
        return Math.max(0, contentHeight(rows) - Math.max(0, height - HEADER_HEIGHT - FOOTER_HEIGHT));
    }

    private static int color(int group) {
        return switch (group) {
            case 0 -> VANILLA_COLOR;
            case 1 -> OWN_COLOR;
            default -> OTHER_COLOR;
        };
    }
}
