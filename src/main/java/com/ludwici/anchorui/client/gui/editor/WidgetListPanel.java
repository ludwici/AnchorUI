package com.ludwici.anchorui.client.gui.editor;

import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.client.gui.NativeContainerScrollPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public final class WidgetListPanel {
    public record Entry(String id, boolean visible, boolean readOnly, String type, String containerParent) {
        public Entry(String id, boolean visible, boolean readOnly) {
            this(id, visible, readOnly, "", null);
        }
    }

    private record Row(Entry entry, int group, String header, int depth, boolean hasChildren) {
        private boolean heading() {
            return entry == null;
        }
    }

    private static final int HEADER_HEIGHT = 21;
    private static final int COLLAPSED_WIDTH = 100;
    private static final int FOOTER_HEIGHT = 17;
    private static final int ROW_HEIGHT = 17;
    private static final int GROUP_HEIGHT = ROW_HEIGHT;
    private static final int PANEL_BACKGROUND = 0xFF121D2B;
    private static final int HEADER_BACKGROUND = 0xFF29445B;
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
    private NativeContainerScrollPanel scrollBar;
    private double previousScrollMouseY;
    private int viewportWidth;
    private int viewportHeight;
    private int startX;
    private int startY;
    private double dragStartX;
    private double dragStartY;
    private boolean dragging;
    private boolean collapsed = true;
    private final Set<Integer> collapsedGroups = new HashSet<>();
    private final Set<String> collapsedContainers = new HashSet<>();
    private boolean rowsDirty = true;
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
        return mouseX >= x && mouseX < x + visibleWidth() && mouseY >= y && mouseY < y + visibleHeight();
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean value) {
        collapsed = value;
        dragging = false;
        if (collapsed && scrollBar != null && scrollBar.isDraggingBar()) {
            scrollBar.mouseReleased(0, 0, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        }
    }

    public boolean overlaps(int left, int top, int right, int bottom) {
        return x < right && x + visibleWidth() > left && y < bottom && y + visibleHeight() > top;
    }

    public boolean isGroupCollapsed(int group) {
        return collapsedGroups.contains(group);
    }

    public boolean beginClick(double mouseX, double mouseY, int button, List<Entry> entries) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return true;
        }
        if (mouseY < y + HEADER_HEIGHT) {
            if (mouseX >= x + visibleWidth() - 27) {
                setCollapsed(!collapsed);
                return true;
            }
            dragging = true;
            startX = x;
            startY = y;
            dragStartX = mouseX;
            dragStartY = mouseY;
            return true;
        }

        if (!collapsed && mouseY < y + height - FOOTER_HEIGHT) {
            ensureScrollBar(rows(entries));
            if (scrollBar != null && scrollBar.mouseClicked(mouseX, mouseY, button)) {
                previousScrollMouseY = mouseY;
                return true;
            }
            Row row = rowAt(mouseY, entries);
            if (row != null && row.heading()) {
                if (!collapsedGroups.add(row.group())) {
                    collapsedGroups.remove(row.group());
                }
                rowsDirty = true;
                ensureScrollBar(rows(entries));
            } else if (row != null && row.hasChildren()
                    && mouseX >= x + 8 + Math.min(60, row.depth() * 10)
                    && mouseX < x + 24 + Math.min(60, row.depth() * 10)) {
                if (!collapsedContainers.add(row.entry().id())) {
                    collapsedContainers.remove(row.entry().id());
                }
                rowsDirty = true;
                ensureScrollBar(rows(entries));
            }
        }
        return true;
    }

    public boolean dragged(double mouseX, double mouseY, int button) {
        if (scrollBar != null && scrollBar.isDraggingBar()) {
            boolean moved = scrollBar.mouseDragged(mouseX, mouseY, button, 0, mouseY - previousScrollMouseY);
            previousScrollMouseY = mouseY;
            scroll = scrollBar.logicalOffset();
            return moved;
        }
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        x = Math.max(0, Math.min(startX + (int) Math.round(mouseX - dragStartX), viewportWidth - width));
        y = Math.max(0, Math.min(startY + (int) Math.round(mouseY - dragStartY), viewportHeight - height));
        return true;
    }

    public boolean released(int button) {
        if (scrollBar != null && scrollBar.isDraggingBar()) {
            scrollBar.mouseReleased(0, 0, button);
            scroll = scrollBar.logicalOffset();
            return true;
        }
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        dragging = false;
        return true;
    }

    public boolean scroll(double mouseX, double mouseY, double scrollY, List<Entry> entries) {
        if (collapsed || !contains(mouseX, mouseY)) {
            return false;
        }
        ensureScrollBar(rows(entries));
        if (scrollBar != null) {
            scrollBar.mouseScrolled(mouseX, mouseY, 0, scrollY);
            scroll = scrollBar.logicalOffset();
        }
        return true;
    }

    public Entry entryAt(double mouseX, double mouseY, List<Entry> entries) {
        if (!contains(mouseX, mouseY) || mouseY < y + HEADER_HEIGHT
                || mouseY >= y + height - FOOTER_HEIGHT) {
            return null;
        }
        Row row = rowAt(mouseY, entries);
        if (row == null || row.heading()) {
            return null;
        }
        if (row.hasChildren() && mouseX >= x + 8 + Math.min(60, row.depth() * 10)
                && mouseX < x + 24 + Math.min(60, row.depth() * 10)) {
            return null;
        }
        return row.entry();
    }

    private int visibleHeight() {
        return collapsed ? HEADER_HEIGHT : height;
    }

    private int visibleWidth() {
        return collapsed ? Math.min(COLLAPSED_WIDTH, width) : width;
    }

    private Row rowAt(double mouseY, List<Entry> entries) {
        ensureScrollBar(rows(entries));
        int pointerY = (int) mouseY - y - HEADER_HEIGHT + scroll;
        int index = pointerY / ROW_HEIGHT;
        List<Row> visibleRows = rows(entries);
        return pointerY >= 0 && index < visibleRows.size() ? visibleRows.get(index) : null;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY,
                       List<Entry> entries, String selected, boolean choosingParent) {
        List<Row> rows = rows(entries);
        ensureScrollBar(rows);
        graphics.fill(x, y, x + visibleWidth(), y + visibleHeight(), PANEL_BACKGROUND);
        graphics.fill(x, y, x + visibleWidth(), y + HEADER_HEIGHT, HEADER_BACKGROUND);
        String title = collapsed ? "Widgets" : "Widgets (" + entries.size() + ")";
        graphics.drawString(font, title, x + 7, y + 7, 0xFFFFFFFF, false);
        graphics.drawString(font, collapsed ? "[+]" : "[-]", x + visibleWidth() - 26, y + 7, 0xFFFFFFFF, false);
        if (collapsed) {
            return;
        }

        int top = y + HEADER_HEIGHT;
        int bottom = y + height - FOOTER_HEIGHT;
        graphics.enableScissor(x + 2, top, x + width - 3, bottom);
        try {
            int first = Math.max(0, scroll / ROW_HEIGHT);
            int last = Math.min(rows.size(), (scroll + bottom - top) / ROW_HEIGHT + 2);
            for (int index = first; index < last; index++) {
                Row row = rows.get(index);
                int lineY = top + index * ROW_HEIGHT - scroll;
                if (lineY + ROW_HEIGHT > top && lineY < bottom) {
                    if (row.heading()) {
                        graphics.fill(x + 3, lineY, x + width - 4, lineY + ROW_HEIGHT, 0xFF223244);
                        String groupTitle = (collapsedGroups.contains(row.group()) ? "[+] " : "[-] ") + row.header();
                        graphics.drawString(font, font.plainSubstrByWidth(groupTitle, width - 18),
                                x + 8, lineY + 4, color(row.group()), false);
                    } else {
                        Entry entry = row.entry();
                        boolean chosen = entry.id().equals(selected);
                        boolean hovering = mouseX >= x + 3 && mouseX < x + width - 4
                                && mouseY >= Math.max(top, lineY) && mouseY < Math.min(bottom, lineY + ROW_HEIGHT);
                        if (chosen || hovering) {
                            graphics.fill(x + 3, lineY, x + width - 5, lineY + ROW_HEIGHT,
                                    chosen ? 0xB04A4435 : 0x88405062);
                        }
                        int tint = chosen ? SELECTED_COLOR : entry.visible() ? color(row.group()) : 0xFF8993A1;
                        int indentation = Math.min(60, row.depth() * 10);
                        int textX = x + 15 + indentation + (row.hasChildren() ? 10 : 0);
                        if (row.hasChildren()) {
                            graphics.drawString(font, collapsedContainers.contains(entry.id()) ? ">" : "v",
                                    x + 11 + indentation, lineY + 4, tint, false);
                        }
                        graphics.fill(textX - 3, lineY + 5, textX, lineY + 12, tint);
                        String local = entry.id().substring(entry.id().indexOf(':') + 1);
                        String label = local.substring(local.lastIndexOf('/') + 1);
                        String prefix = widgetPrefix(entry.type());
                        label = font.plainSubstrByWidth(prefix + label, width - (textX - x) - 16);
                        graphics.drawString(font, label, textX + 4, lineY + 4, tint, false);
                    }
                }
            }
        } finally {
            graphics.disableScissor();
        }
        graphics.fill(x + 3, bottom, x + width - 3, bottom + 1, 0xFF50647A);
        graphics.drawString(font, choosingParent ? "Click a parent ID" : "Click to select | Scroll",
                x + 6, bottom + 5, 0xFFB7C8D6, false);

        if (scrollBar != null) {
            scrollBar.render(graphics, mouseX, mouseY, 0);
        }

        Entry hovered = entryAt(mouseX, mouseY, entries);
        if (hovered != null) {
            graphics.renderTooltip(font, Component.literal(hovered.id()
                    + (hovered.type().isEmpty() ? "" : " [" + hovered.type() + "]")
                    + (hovered.readOnly() ? " (read-only)" : "")), mouseX, mouseY);
        }
    }

    private void ensureScrollBar(List<Row> rows) {
        int top = y + HEADER_HEIGHT;
        int bottom = y + height - FOOTER_HEIGHT;
        int max = maxScroll(rows);
        if (max <= 0 || bottom - top < 34 || width <= 16) {
            scrollBar = null;
            scroll = 0;
            return;
        }
        AnchorLayout.Bounds viewport = new AnchorLayout.Bounds(x + 3, top, width - 6, bottom - top);
        if (scrollBar == null || !scrollBar.matches(viewport, max, ROW_HEIGHT * 2, 1.0, NativeContainerScrollPanel.WIDGET_LIST)) {
            scrollBar = new NativeContainerScrollPanel(Minecraft.getInstance(), viewport,
                    max, ROW_HEIGHT * 2, 1.0, scroll, NativeContainerScrollPanel.WIDGET_LIST);
        }
        scroll = scrollBar.logicalOffset();
    }

    private List<Row> rows(List<Entry> entries) {
        if (!rowsDirty && entries.equals(cachedEntries)) {
            return cachedRows;
        }
        Map<String, Entry> byId = new LinkedHashMap<>();
        for (Entry entry : entries) {
            byId.putIfAbsent(entry.id(), entry);
        }
        List<String> sorted = EditorWidgetOrder.sorted(byId.keySet(), currentNamespace);
        List<Row> result = new ArrayList<>();
        int previousGroup = -1;
        for (String id : sorted) {
            int group = EditorWidgetOrder.group(id, currentNamespace);
            if (group != previousGroup) {
                String heading = switch (group) {
                    case 0 -> "VANILLA";
                    case 1 -> "CURRENT: " + currentNamespace;
                    default -> "OTHER MODS";
                };
                result.add(new Row(null, group, heading, 0, false));
                previousGroup = group;
            }
            if (collapsedGroups.contains(group)) {
                continue;
            }
            Entry entry = byId.get(id);
            if (entry.containerParent() != null && byId.containsKey(entry.containerParent())
                    && EditorWidgetOrder.group(entry.containerParent(), currentNamespace) == group) {
                continue;
            }
            appendHierarchy(result, entry, group, 0, byId, sorted, new HashSet<>());
        }
        Set<String> seen = new HashSet<>();
        for (Row row : result) {
            if (row.entry() != null) {
                seen.add(row.entry().id());
            }
        }
        for (String id : sorted) {
            int group = EditorWidgetOrder.group(id, currentNamespace);
            if (!collapsedGroups.contains(group) && !seen.contains(id)
                    && !isUnderCollapsedParent(byId.get(id), byId)) {
                appendHierarchy(result, byId.get(id), group, 0, byId, sorted, seen);
            }
        }
        cachedEntries = List.copyOf(entries);
        cachedRows = List.copyOf(result);
        rowsDirty = false;
        return cachedRows;
    }

    private void appendHierarchy(List<Row> rows, Entry entry, int group, int depth,
                                 Map<String, Entry> byId, List<String> ordered, Set<String> seen) {
        if (!seen.add(entry.id())) {
            return;
        }
        boolean hasChildren = false;
        for (String candidate : ordered) {
            Entry child = byId.get(candidate);
            if (entry.id().equals(child.containerParent())
                    && EditorWidgetOrder.group(child.id(), currentNamespace) == group) {
                hasChildren = true;
                break;
            }
        }
        rows.add(new Row(entry, group, null, depth, hasChildren));
        if (collapsedContainers.contains(entry.id()) || depth >= 12) {
            return;
        }
        for (String candidate : ordered) {
            Entry child = byId.get(candidate);
            if (entry.id().equals(child.containerParent())
                    && EditorWidgetOrder.group(child.id(), currentNamespace) == group) {
                appendHierarchy(rows, child, group, depth + 1, byId, ordered, seen);
            }
        }
    }

    private boolean isUnderCollapsedParent(Entry entry, Map<String, Entry> byId) {
        Set<String> visited = new HashSet<>();
        String parent = entry.containerParent();
        while (parent != null && visited.add(parent)) {
            if (collapsedContainers.contains(parent)) {
                return true;
            }
            Entry ancestor = byId.get(parent);
            parent = ancestor == null ? null : ancestor.containerParent();
        }
        return false;
    }

    private static String widgetPrefix(String type) {
        if (type.endsWith(":panel") || type.endsWith(":container")) {
            return "[P] ";
        }
        if (type.endsWith(":button")) {
            return "[B] ";
        }
        if (type.endsWith(":text")) {
            return "[T] ";
        }
        if (type.endsWith(":progress")) {
            return "[=] ";
        }
        if (type.endsWith(":image") || type.endsWith(":texture")) {
            return "[I] ";
        }
        return "";
    }

    private int contentHeight(List<Row> rows) {
        return rows.size() * ROW_HEIGHT;
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
