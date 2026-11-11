package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.KeyRegistry;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.client.hud.AnchorHudManager;
import com.ludwici.anchorui.client.hud.HudInspectorProperties;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.client.hud.HudSnap;
import com.ludwici.anchorui.client.gui.NativeContainerScrollPanel;
import com.ludwici.anchorui.client.gui.editor.WidgetListPanel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class AnchorScreenEditorScreen extends Screen {
    private static final int WIDTH = 222;
    private static final int BACKGROUND = 0xFF18232F;
    private static final int OUTLINE = 0xFF67D4D8;
    private static final int SELECTED = 0xFFFFD16B;
    private static final int HIDDEN = 0xFF929BA7;
    private static final int GUIDES = 0xFF8DF0D3;
    private static final int INSPECTOR_LINE_HEIGHT = 11;
    private static final int RESET_SIZE = 11;
    private static final float INSPECTOR_SCALE = 0.83F;

    private record InspectorLine(FormattedCharSequence text, int color, AnchorHudManager.EditableProperty property, boolean modified) {
    }

    private record ResetHitbox(AnchorHudManager.EditableProperty property, int x, int y) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + RESET_SIZE && mouseY >= y && mouseY < y + RESET_SIZE;
        }
    }

    private final ItemLoreScreen target;
    private final WidgetListPanel widgetList;
    private List<ItemLoreScreen.EditorHandle> frameHandles = List.of();
    private List<WidgetListPanel.Entry> frameEntries = List.of();
    private final List<Button> controls = new ArrayList<>();
    private final List<ResetHitbox> resetHitboxes = new ArrayList<>();
    private List<String> sourceProperties = List.of();
    private String inspectedId;
    private int inspectorScroll;
    private int maxInspectorScroll;
    private NativeContainerScrollPanel inspectorScrollBar;
    private ItemLoreScreen.EditorHandle selected;
    private Button hide;
    private Button parent;
    private Button reset;
    private Button point;
    private Button anchor;
    private boolean pickingParent;
    private boolean compact;
    private boolean inspectorCollapsed;
    private int inspectorWidth = WIDTH;
    private boolean draggingPanel;
    private int panelLeft = -1;
    private int panelTop = 12;
    private double panelStartX;
    private double panelStartY;
    private int panelDragLeft;
    private int panelDragTop;
    private boolean dragging;
    private HudSnap.Widget dragOrigin;
    private HudSnap.Result snapped;
    private double startX;
    private double startY;
    private HudLayoutOffsets.Offset startOffset;
    private String status = "";

    public AnchorScreenEditorScreen(ItemLoreScreen target) {
        super(Component.literal("Anchor UI Screen Editor"));
        this.target = target;
        this.widgetList = new WidgetListPanel(target.editorNamespace());
    }

    @Override
    protected void init() {
        if (!target.isScreenEditing()) {
            target.beginScreenEditing();
        }
        controls.clear();
        widgetList.resize(width, height);
        inspectorWidth = Math.min(WIDTH, Math.max(150, width - 116));
        if (panelLeft < 0) {
            panelLeft = Math.max(2, width - inspectorWidth - 8);
        }
        panelLeft = Math.max(0, Math.min(panelLeft, width - inspectorWidth));
        panelTop = Math.max(0, Math.min(panelTop, height - 155));
        if (!inspectorCollapsed && widgetList.overlaps(panelX(), panelTop,
                panelX() + inspectorWidth, panelTop + expandedPanelHeight())) {
            inspectorCollapsed = true;
        }
        int x = panelX();
        int y = panelTop;
        add(x + 7, y + 29, 100, "Save", button -> save());
        add(x + 114, y + 29, 100, "Cancel", button -> cancel());
        hide = add(x + 7, y + 53, 100, "Hide", button -> toggleHidden());
        reset = add(x + 114, y + 53, 100, "Reset", button -> reset());
        point = add(x + 7, y + 77, 100, "Point", button -> cyclePoint());
        anchor = add(x + 114, y + 77, 100, "Anchor", button -> cycleAnchor());
        parent = add(x + 7, y + 101, 100, "Parent", button -> pickParent());
        add(x + 114, y + 101, 100, "Reset all", button -> resetAll());
        positionControls();
        updateControls();
    }

    private Button add(int x, int y, int width, String label, Button.OnPress action) {
        Button button = Button.builder(Component.literal(label), action).bounds(x, y, width, 20).build();
        controls.add(button);
        addRenderableWidget(button);
        return button;
    }

    private int panelX() {
        return Math.max(0, panelLeft);
    }

    private int expandedPanelHeight() {
        return Math.max(155, Math.min(390, height - panelTop - 8));
    }

    private int inspectorTop() {
        return panelTop + 132;
    }

    private int inspectorBottom() {
        return panelTop + expandedPanelHeight() - 22;
    }

    private boolean insideInspector(double x, double y) {
        return !compact && !inspectorCollapsed && x >= panelX() && x < panelX() + inspectorWidth
                && y >= inspectorTop() && y < inspectorBottom();
    }

    private int visiblePanelX() {
        return inspectorCollapsed ? panelX() + inspectorWidth - 104 : panelX();
    }

    private int visiblePanelWidth() {
        return inspectorCollapsed ? 104 : inspectorWidth;
    }

    private void toggleInspector() {
        if (inspectorCollapsed && widgetList.overlaps(panelX(), panelTop,
                panelX() + inspectorWidth, panelTop + expandedPanelHeight())) {
            widgetList.setCollapsed(true);
        }
        inspectorCollapsed = !inspectorCollapsed;
        draggingPanel = false;
        updateControls();
    }

    private void positionControls() {
        int halfWidth = (inspectorWidth - 21) / 2;
        for (int index = 0; index < controls.size(); index++) {
            Button button = controls.get(index);
            button.setWidth(halfWidth);
            button.setPosition(panelX() + 7 + (index % 2) * (halfWidth + 7),
                    panelTop + 29 + (index / 2) * 24);
        }
    }

    private void updateControls() {
        boolean hasSelection = selected != null;
        hide.active = hasSelection;
        reset.active = hasSelection;
        point.active = hasSelection;
        anchor.active = hasSelection;
        parent.active = hasSelection;
        hide.setMessage(Component.literal(hasSelection && selected.hiddenByUser() ? "Show" : "Hide"));
        parent.setMessage(Component.literal(pickingParent ? "Pick target..." : "Parent"));
        for (Button button : controls) {
            button.visible = !compact && !inspectorCollapsed;
        }
    }

    private void select(String id) {
        if (id == null || !id.equals(inspectedId)) {
            inspectedId = null;
            sourceProperties = List.of();
            resetInspectorScroll();
        }
        selected = null;
        if (id != null) {
            for (ItemLoreScreen.EditorHandle handle : target.editorHandles()) {
                if (handle.id().equals(id)) {
                    selected = handle;
                    break;
                }
            }
        }
        updateControls();
    }

    private void refreshFrameHandles() {
        frameHandles = target.editorHandles();
        List<WidgetListPanel.Entry> entries = new ArrayList<>(frameHandles.size());
        for (ItemLoreScreen.EditorHandle handle : frameHandles) {
            entries.add(new WidgetListPanel.Entry(handle.id(), handle.visible(), false));
        }
        frameEntries = List.copyOf(entries);
        if (selected != null) {
            for (ItemLoreScreen.EditorHandle handle : frameHandles) {
                if (handle.id().equals(selected.id())) {
                    selected = handle;
                    break;
                }
            }
        }
    }

    private void selectFromList(WidgetListPanel.Entry entry) {
        if (pickingParent && selected != null) {
            if (target.editorAnchor(selected.id(), entry.id(), selected.point(), selected.anchor())) {
                pickingParent = false;
                status = "Parent set to " + entry.id();
            } else {
                status = "Invalid or circular parent: " + entry.id();
            }
            refreshSelection();
            updateControls();
            return;
        }
        select(entry.id());
        status = "";
        dragging = false;
        dragOrigin = null;
        snapped = null;
    }

    private ItemLoreScreen.EditorHandle hit(double x, double y) {
        List<ItemLoreScreen.EditorHandle> handles = frameHandles;
        for (int i = handles.size() - 1; i >= 0; i--) {
            ItemLoreScreen.EditorHandle handle = handles.get(i);
            AnchorLayout.Bounds b = handle.bounds();
            if (b.width() > 0 && b.height() > 0 && x >= b.x() && x < b.x() + b.width()
                    && y >= b.y() && y < b.y() + b.height()) {
                return handle;
            }
        }
        return null;
    }

    private void refreshSelection() {
        if (selected != null) {
            select(selected.id());
        }
    }

    private void save() {
        if (target.saveScreenEditing()) {
            minecraft.setScreen(target);
        } else {
            status = target.screenSaveError();
        }
    }

    private void cancel() {
        target.cancelScreenEditing();
        minecraft.setScreen(target);
    }

    private void toggleHidden() {
        if (selected != null) {
            target.editorHidden(selected.id(), !selected.hiddenByUser());
            refreshSelection();
        }
    }

    private void reset() {
        if (selected != null && target.resetEditorWidget(selected.id())) {
            refreshSelection();
        }
    }

    private void resetAll() {
        target.resetAllEditorWidgets();
        refreshSelection();
    }

    private void cyclePoint() {
        if (selected != null) {
            ElementAnchor[] values = ElementAnchor.values();
            ElementAnchor next = values[(selected.point().ordinal() + 1) % values.length];
            if (!target.editorAnchor(selected.id(), selected.relativeTo(), next, selected.anchor())) {
                status = "Invalid point";
            }
            refreshSelection();
        }
    }

    private void cycleAnchor() {
        if (selected != null) {
            ElementAnchor[] values = ElementAnchor.values();
            ElementAnchor next = values[(selected.anchor().ordinal() + 1) % values.length];
            if (!target.editorAnchor(selected.id(), selected.relativeTo(), selected.point(), next)) {
                status = "Invalid anchor";
            }
            refreshSelection();
        }
    }

    private void pickParent() {
        pickingParent = !pickingParent;
        status = pickingParent ? "Click a widget, or empty space for screen" : "";
        updateControls();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        target.render(graphics, -1, -1, partialTick);
        refreshFrameHandles();
        for (ItemLoreScreen.EditorHandle handle : frameHandles) {
            AnchorLayout.Bounds b = handle.bounds();
            int color = selected != null && selected.id().equals(handle.id()) ? SELECTED : handle.visible() ? OUTLINE : HIDDEN;
            outline(graphics, b.x(), b.y(), b.width(), b.height(), color);
            if (selected != null && selected.id().equals(handle.id())) {
                graphics.drawString(font, handle.id(), b.x(), b.y() - 10, color, false);
            }
        }
        if (dragging && snapped != null) {
            if (snapped.guideX() != null) {
                graphics.fill(snapped.guideX(), 0, snapped.guideX() + 1, height, GUIDES);
            }
            if (snapped.guideY() != null) {
                graphics.fill(0, snapped.guideY(), width, snapped.guideY() + 1, GUIDES);
            }
        }
        if (compact) {
            graphics.drawString(font, "Screen Editor (Tab: show panel, F8: save)", 8, 7, SELECTED, false);
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        try {
            int x = visiblePanelX();
            int y = panelTop;
            int panelHeight = inspectorCollapsed ? 27 : expandedPanelHeight();
            graphics.fill(x, y, x + visiblePanelWidth(), y + panelHeight, BACKGROUND);
            graphics.fill(x, y, x + visiblePanelWidth(), y + 1, OUTLINE);
            graphics.drawString(font, inspectorCollapsed ? "Inspector" : "Screen Editor",
                    x + 8, y + 9, 0xFFFFFFFF, false);
            graphics.drawString(font, inspectorCollapsed ? "[+]" : "[-]",
                    x + visiblePanelWidth() - 27, y + 9, 0xFFFFFFFF, false);
            if (!inspectorCollapsed) {
                graphics.drawString(font, pickingParent ? "Pick a widget or empty space" :
                        "Drag: magnet 6px | Alt: free", x + 8, y + 123, 0xFFACD9D9, false);
                renderInspector(graphics);
                if (!status.isEmpty()) {
                    String message = font.plainSubstrByWidth(status, inspectorWidth - 18);
                    graphics.drawString(font, message, x + 8, y + expandedPanelHeight() - 14,
                            0xFFFFD16B, false);
                }
            }
            super.render(graphics, mouseX, mouseY, partialTick);
            widgetList.render(graphics, font, mouseX, mouseY, frameEntries,
                    selected == null ? null : selected.id(), pickingParent);
        } finally {
            graphics.pose().popPose();
        }
    }

    private void renderInspector(GuiGraphics graphics) {
        int top = inspectorTop();
        int bottom = inspectorBottom();
        resetHitboxes.clear();
        if (bottom <= top) {
            return;
        }
        List<InspectorLine> lines = inspectorLines();
        maxInspectorScroll = Math.max(0, lines.size() * INSPECTOR_LINE_HEIGHT - (bottom - top));
        inspectorScroll = Math.max(0, Math.min(inspectorScroll, maxInspectorScroll));
        updateInspectorScrollBar(top, bottom);
        graphics.enableScissor(panelX() + 4, top, panelX() + inspectorWidth - 4, bottom);
        try {
            int lineY = top - inspectorScroll;
            for (InspectorLine line : lines) {
                if (lineY + INSPECTOR_LINE_HEIGHT > top && lineY < bottom) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(panelX() + 7, lineY + 1, 0);
                    graphics.pose().scale(INSPECTOR_SCALE, INSPECTOR_SCALE, 1.0F);
                    graphics.drawString(font, line.text(), 0, 0, line.color(), false);
                    graphics.pose().popPose();

                    if (line.property() != null) {
                        int buttonX = panelX() + inspectorWidth - 21;
                        int background = line.modified() ? 0xFF3D6273 : 0xFF40505D;
                        int foreground = line.modified() ? 0xFFECF8FF : 0xFF80919E;
                        graphics.fill(buttonX, lineY, buttonX + RESET_SIZE, lineY + RESET_SIZE, background);
                        graphics.drawString(font, "R", buttonX + 2, lineY + 1, foreground, false);
                        if (line.modified() && lineY >= top && lineY + RESET_SIZE <= bottom) {
                            resetHitboxes.add(new ResetHitbox(line.property(), buttonX, lineY));
                        }
                    }
                }
                lineY += INSPECTOR_LINE_HEIGHT;
            }
        } finally {
            graphics.disableScissor();
        }
        if (inspectorScrollBar != null) {
            inspectorScrollBar.render(graphics, 0, 0, 0);
        }
    }

    private List<InspectorLine> inspectorLines() {
        List<InspectorLine> lines = new ArrayList<>();
        if (selected == null) {
            appendInspectorLine(lines, "SELECT A WIDGET", SELECTED, null, false);
            appendInspectorLine(lines, "Click a widget or the Widgets list", 0xFFE5EEF5, null, false);
            appendInspectorLine(lines, "Arrows: 1px | Shift: 10px", 0xFFE5EEF5, null, false);
            appendInspectorLine(lines, "Center X / Y: magnetic guides", 0xFFE5EEF5, null, false);
            appendInspectorLine(lines, "Ctrl+S: save | Esc: cancel", 0xFFE5EEF5, null, false);
            return lines;
        }

        appendInspectorLine(lines, "CURRENT WIDGET", SELECTED, null, false);
        appendInspectorLine(lines, "R: reset individual property", 0xFF96AEBE, null, false);
        appendInspectorLine(lines, "id: " + selected.id(), 0xFFE5EEF5, null, false);
        appendProperty(lines, "parent: " + selected.relativeTo(), AnchorHudManager.EditableProperty.PARENT);
        appendProperty(lines, "point: " + selected.point(), AnchorHudManager.EditableProperty.POINT);
        appendProperty(lines, "anchor: " + selected.anchor(), AnchorHudManager.EditableProperty.ANCHOR);
        AnchorLayout.Bounds bounds = selected.bounds();
        appendInspectorLine(lines, "screen: " + bounds.x() + ", " + bounds.y(), 0xFFE5EEF5, null, false);
        appendInspectorLine(lines, "size: " + bounds.width() + " x " + bounds.height(), 0xFFE5EEF5, null, false);
        appendProperty(lines, "user offset: " + selected.offset().x() + ", " + selected.offset().y(),
                AnchorHudManager.EditableProperty.OFFSET);
        appendProperty(lines, "visible: " + selected.visible(), AnchorHudManager.EditableProperty.VISIBILITY);
        if (!selected.id().equals(inspectedId)) {
            JsonObject settings = target.editorSourceSettings(selected.id());
            sourceProperties = HudInspectorProperties.flatten(settings);
            inspectedId = selected.id();
        }
        appendInspectorLine(lines, "SOURCE JSON (read only)", SELECTED, null, false);
        for (String property : sourceProperties) {
            appendInspectorLine(lines, property, 0xFFB7CEDF, null, false);
        }
        return lines;
    }

    private void appendProperty(List<InspectorLine> lines, String label, AnchorHudManager.EditableProperty property) {
        appendInspectorLine(lines, label, 0xFFE5EEF5, property, selected.isModified(property));
    }

    private void appendInspectorLine(List<InspectorLine> lines, String label, int color,
                                     AnchorHudManager.EditableProperty property, boolean modified) {
        int reserved = property == null ? 20 : 39;
        int available = Math.max(20, (int) ((inspectorWidth - reserved) / INSPECTOR_SCALE));
        boolean first = true;
        for (FormattedCharSequence part : font.split(Component.literal(label), available)) {
            lines.add(new InspectorLine(part, color, first ? property : null, modified));
            first = false;
        }
    }

    private void updateInspectorScrollBar(int top, int bottom) {
        if (maxInspectorScroll <= 0 || bottom - top < 34 || inspectorWidth < 20) {
            inspectorScrollBar = null;
            inspectorScroll = 0;
            return;
        }
        AnchorLayout.Bounds viewport = new AnchorLayout.Bounds(panelX() + 4, top, inspectorWidth - 7, bottom - top);
        if (inspectorScrollBar == null || !inspectorScrollBar.matches(viewport, maxInspectorScroll, 23, 1.0, NativeContainerScrollPanel.SCREEN_INSPECTOR)) {
            inspectorScrollBar = new NativeContainerScrollPanel(minecraft, viewport, maxInspectorScroll, 23, 1.0,
                    inspectorScroll, NativeContainerScrollPanel.SCREEN_INSPECTOR);
        }
        inspectorScroll = inspectorScrollBar.logicalOffset();
    }

    private void resetInspectorScroll() {
        inspectorScroll = 0;
        if (inspectorScrollBar != null) {
            inspectorScrollBar.setLogicalOffset(0);
        }
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!compact && button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && mouseX >= visiblePanelX() + visiblePanelWidth() - 30
                && mouseX < visiblePanelX() + visiblePanelWidth()
                && mouseY >= panelTop && mouseY < panelTop + 27) {
            toggleInspector();
            return true;
        }
        if (!compact && widgetList.contains(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                WidgetListPanel.Entry entry = widgetList.entryAt(mouseX, mouseY, frameEntries);
                if (entry != null) {
                    selectFromList(entry);
                }
            }
            boolean wasCollapsed = widgetList.isCollapsed();
            widgetList.beginClick(mouseX, mouseY, button, frameEntries);
            if (wasCollapsed && !widgetList.isCollapsed() && !inspectorCollapsed
                    && widgetList.overlaps(panelX(), panelTop,
                    panelX() + inspectorWidth, panelTop + expandedPanelHeight())) {
                toggleInspector();
            }
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!compact && mouseX >= visiblePanelX()
                && mouseX < visiblePanelX() + visiblePanelWidth()
                && mouseY >= panelTop && mouseY < panelTop + (inspectorCollapsed ? 27 : expandedPanelHeight())) {
            if (insideInspector(mouseX, mouseY)) {
                for (ResetHitbox hitbox : resetHitboxes) {
                    if (hitbox.contains(mouseX, mouseY)) {
                        if (selected != null && target.resetEditorProperty(selected.id(), hitbox.property())) {
                            status = "Restored " + hitbox.property().name().toLowerCase(java.util.Locale.ROOT);
                            refreshSelection();
                        }
                        return true;
                    }
                }
                if (inspectorScrollBar != null) {
                    inspectorScrollBar.mouseClicked(mouseX, mouseY, button);
                }
            } else if (mouseY < panelTop + 27) {
                draggingPanel = true;
                panelStartX = mouseX;
                panelStartY = mouseY;
                panelDragLeft = panelLeft;
                panelDragTop = panelTop;
            }
            return true;
        }
        if (target.mouseClickedScrollBar(mouseX, mouseY, button)) {
            dragging = false;
            dragOrigin = null;
            snapped = null;
            return true;
        }
        ItemLoreScreen.EditorHandle hit = hit(mouseX, mouseY);
        if (pickingParent && selected != null) {
            String parent = hit == null ? "screen" : hit.id();
            if (target.editorAnchor(selected.id(), parent, selected.point(), selected.anchor())) {
                status = "";
            } else {
                status = "Invalid or circular parent";
            }
            pickingParent = false;
            refreshSelection();
            return true;
        }
        select(hit == null ? null : hit.id());
        if (selected != null) {
            AnchorLayout.Bounds bounds = selected.bounds();
            dragOrigin = new HudSnap.Widget(selected.id(), selected.relativeTo(), bounds.x(), bounds.y(),
                    bounds.width(), bounds.height(), selected.visible());
            startX = mouseX;
            startY = mouseY;
            startOffset = selected.offset();
            dragging = true;
            snapped = null;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (inspectorScrollBar != null && inspectorScrollBar.isDraggingBar()) {
            inspectorScrollBar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            inspectorScroll = inspectorScrollBar.logicalOffset();
            return true;
        }
        if (target.mouseDraggedScrollBar(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (widgetList.dragged(mouseX, mouseY, button)) {
            return true;
        }
        if (draggingPanel && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            panelLeft = Math.max(0, Math.min(width - inspectorWidth, panelDragLeft + (int) Math.round(mouseX - panelStartX)));
            panelTop = Math.max(0, Math.min(height - expandedPanelHeight(),
                    panelDragTop + (int) Math.round(mouseY - panelStartY)));
            positionControls();
            return true;
        }
        if (!dragging || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || selected == null || dragOrigin == null) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        int desiredX = dragOrigin.x() + (int) Math.round(mouseX - startX);
        int desiredY = dragOrigin.y() + (int) Math.round(mouseY - startY);
        if (hasAltDown()) {
            snapped = new HudSnap.Result(desiredX, desiredY, null, null);
        } else {
            List<HudSnap.Widget> targets = new ArrayList<>();
            for (ItemLoreScreen.EditorHandle handle : target.editorHandles()) {
                AnchorLayout.Bounds b = handle.bounds();
                targets.add(new HudSnap.Widget(handle.id(), handle.relativeTo(), b.x(), b.y(),
                        b.width(), b.height(), handle.visible()));
            }
            snapped = HudSnap.snap(dragOrigin, desiredX, desiredY, targets, width, height);
        }
        double scale = target.editorDragScale(selected.id());
        int nextX = startOffset.x() + (int) Math.round((snapped.x() - dragOrigin.x()) / scale);
        int nextY = startOffset.y() + (int) Math.round((snapped.y() - dragOrigin.y()) / scale);
        target.editorOffset(selected.id(), nextX, nextY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (inspectorScrollBar != null && inspectorScrollBar.isDraggingBar()) {
            inspectorScrollBar.mouseReleased(mouseX, mouseY, button);
            inspectorScroll = inspectorScrollBar.logicalOffset();
            return true;
        }
        if (target.mouseReleasedScrollBar(mouseX, mouseY, button)) {
            return true;
        }
        if (widgetList.released(button)) {
            return true;
        }
        if (draggingPanel && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingPanel = false;
            return true;
        }
        if (dragging && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
            snapped = null;
            dragOrigin = null;
            refreshSelection();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!compact && widgetList.scroll(mouseX, mouseY, scrollY, frameEntries)) {
            return true;
        }
        if (insideInspector(mouseX, mouseY)) {
            if (inspectorScrollBar != null) {
                inspectorScrollBar.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
                inspectorScroll = inspectorScrollBar.logicalOffset();
            }
            return true;
        }
        if (target.scrollContentsAt(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyRegistry.EDIT_HUD.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (pickingParent) {
                pickingParent = false;
                status = "";
                updateControls();
                return true;
            }
            cancel();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            compact = !compact;
            draggingPanel = false;
            updateControls();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_S && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R && selected != null) {
            reset();
            return true;
        }
        if (selected != null) {
            int amount = hasShiftDown() ? 10 : 1;
            int dx = 0;
            int dy = 0;
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> dx = -amount;
                case GLFW.GLFW_KEY_RIGHT -> dx = amount;
                case GLFW.GLFW_KEY_UP -> dy = -amount;
                case GLFW.GLFW_KEY_DOWN -> dy = amount;
                default -> {
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            }
            target.editorOffset(selected.id(), selected.offset().x() + dx, selected.offset().y() + dy);
            refreshSelection();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        cancel();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
