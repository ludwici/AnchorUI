package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.KeyRegistry;
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

public final class AnchorHudEditorScreen extends Screen {
    private static final int SELECTED_COLOR = 0xFFFFCF64;
    private static final int NORMAL_COLOR = 0xFF69D7D9;
    private static final int HIDDEN_COLOR = 0xFF9E9E9E;
    private static final int PARENT_COLOR = 0xFF8DF4AA;
    private static final int SNAP_GUIDE_COLOR = 0xFF88F0D6;
    private static final int VANILLA_COLOR = 0xFFAE9CE9;
    private static final int VANILLA_INACTIVE_COLOR = 0xFF6F6982;
    private static final int PANEL_WIDTH = 218;
    private static final int BUTTON_HEIGHT = 17;
    private static final int BUTTON_STEP = 20;
    private static final int FOOTER_HEIGHT = 85;
    private static final int HEADER_HEIGHT = 34;
    private static final int INSPECTOR_LINE_HEIGHT = 11;
    private static final float INSPECTOR_SCALE = 0.83F;
    private static final int RESET_SIZE = 11;

    private record InspectorLine(FormattedCharSequence text, int color, AnchorHudManager.EditableProperty reset, boolean modified, String vanillaParent) {
    }

    private record VanillaParentHitbox(String id, int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private record ResetHitbox(AnchorHudManager.EditableProperty property, int x, int y) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + RESET_SIZE && mouseY >= y && mouseY < y + RESET_SIZE;
        }
    }

    private final AnchorHudManager manager = AnchorHudManager.INSTANCE;
    private final WidgetListPanel widgetList = new WidgetListPanel("anchorui");
    private List<AnchorHudManager.HudHandle> frameHandles = List.of();
    private List<VanillaHudGeometry.Reference> frameVanillaHandles = List.of();
    private List<WidgetListPanel.Entry> frameEntries = List.of();
    private final List<Button> controls = new ArrayList<>();
    private final List<ResetHitbox> resetHitboxes = new ArrayList<>();
    private final List<VanillaParentHitbox> vanillaParentHitboxes = new ArrayList<>();
    private int vanillaViewMode = 1;
    private String inspectedVanilla;
    private boolean editorStarted;
    private boolean compact;
    private boolean inspectorCollapsed;
    private boolean choosingParent;
    private boolean draggingWidget;
    private boolean draggingPanel;
    private boolean draggingScroll;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int maxInspectorScroll;
    private int inspectorScroll;
    private String selected;
    private String inspectedId;
    private List<String> sourceProperties = List.of();
    private String status = "";
    private double dragStartX;
    private double dragStartY;
    private int startOffsetX;
    private int startOffsetY;
    private HudSnap.Widget dragOrigin;
    private HudSnap.Result snapResult;
    private int dragPanelX;
    private int dragPanelY;
    private Button resetButton;
    private Button hideButton;
    private Button pointButton;
    private Button anchorButton;
    private Button parentButton;
    private Button demoButton;
    private Button vanillaButton;

    public AnchorHudEditorScreen() {
        super(Component.literal("Anchor UI HUD Editor"));
    }

    @Override
    protected void init() {
        draggingWidget = false;
        draggingPanel = false;
        draggingScroll = false;
        dragOrigin = null;
        snapResult = null;
        controls.clear();
        widgetList.resize(width, height);
        if (!editorStarted) {
            manager.beginEditing();
            editorStarted = true;
            panelWidth = Math.min(PANEL_WIDTH, Math.max(150, width - 116));
            panelHeight = Math.min(350, Math.max(128, height - 80));
            panelX = width - panelWidth - 8;
            panelY = 8;
        } else {
            panelWidth = Math.min(PANEL_WIDTH, Math.max(150, width - 116));
            panelHeight = Math.min(350, Math.max(128, height - 80));
            clampPanel();
        }
        if (!inspectorCollapsed && widgetList.overlaps(panelX, panelY, panelX + panelWidth, panelY + panelHeight)) {
            inspectorCollapsed = true;
        }
        int left = panelX + 6;
        int innerWidth = panelWidth - 12;
        int halfWidth = (innerWidth - 4) / 2;
        int right = left + halfWidth + 4;
        int actionsY = actionsTop();

        demoButton = addControl(Button.builder(Component.literal("Demo"), button -> {
            manager.setTestEnabled(!manager.isTestEnabled());
            selectWidget(null);
            choosingParent = false;
            status = manager.isTestEnabled() ? "Demo HUD enabled" : "Demo HUD disabled";
        }).bounds(panelX + panelWidth - 46, panelY + 3, 40, 16).build());

        vanillaButton = addControl(Button.builder(Component.literal("Vanilla"), button -> {
            vanillaViewMode = (vanillaViewMode + 1) % 3;
            status = switch (vanillaViewMode) {
                case 0 -> "Vanilla outlines hidden (references still work)";
                case 1 -> "Vanilla: active HUD elements";
                default -> "Vanilla: all 25 HUD references";
            };
        }).bounds(panelX + panelWidth - 114, panelY + 3, 62, 16).build());

        pointButton = addControl(Button.builder(Component.literal("Point >"), button -> {
            if (selected != null && manager.cyclePreviewPoint(selected)) {
                status = "Widget point changed";
            }
        }).bounds(left, actionsY, halfWidth, BUTTON_HEIGHT).build());
        anchorButton = addControl(Button.builder(Component.literal("Anchor >"), button -> {
            if (selected != null && manager.cyclePreviewAnchor(selected)) {
                status = "Parent anchor changed";
            }
        }).bounds(right, actionsY, halfWidth, BUTTON_HEIGHT).build());
        parentButton = addControl(Button.builder(Component.literal("Pick parent"), button -> {
            if (selected != null) {
                choosingParent = !choosingParent;
                draggingWidget = false;
                status = choosingParent ? "Click a widget or empty space (screen)" : "Parent selection cancelled";
            }
        }).bounds(left, actionsY + BUTTON_STEP, halfWidth, BUTTON_HEIGHT).build());
        hideButton = addControl(Button.builder(Component.literal("Hide"), button -> {
            if (selected != null) {
                manager.togglePreviewHidden(selected);
                status = "Visibility changed";
            }
        }).bounds(right, actionsY + BUTTON_STEP, halfWidth, BUTTON_HEIGHT).build());
        resetButton = addControl(Button.builder(Component.literal("Reset"), button -> {
            if (selected != null) {
                manager.resetPreviewOffset(selected);
                status = "Restored widget defaults";
            }
        }).bounds(left, actionsY + BUTTON_STEP * 2, halfWidth, BUTTON_HEIGHT).build());
        addControl(Button.builder(Component.literal("Reset all"), button -> {
            manager.resetAllPreviewOffsets();
            status = "Restored all HUD defaults";
        }).bounds(right, actionsY + BUTTON_STEP * 2, halfWidth, BUTTON_HEIGHT).build());
        addControl(Button.builder(Component.literal("Save"), button -> saveAndClose()).bounds(left, actionsY + BUTTON_STEP * 3, halfWidth, BUTTON_HEIGHT).build());
        addControl(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(right, actionsY + BUTTON_STEP * 3, halfWidth, BUTTON_HEIGHT).build());
    }

    private Button addControl(Button button) {
        controls.add(button);
        return addRenderableWidget(button);
    }

    private int actionsTop() {
        return panelY + panelHeight - FOOTER_HEIGHT;
    }

    private int visiblePanelHeight() {
        return inspectorCollapsed ? 20 : panelHeight;
    }

    private int visiblePanelX() {
        return inspectorCollapsed ? panelX + panelWidth - 104 : panelX;
    }

    private int visiblePanelWidth() {
        return inspectorCollapsed ? 104 : panelWidth;
    }

    private void toggleInspector() {
        if (inspectorCollapsed && widgetList.overlaps(panelX, panelY, panelX + panelWidth, panelY + panelHeight)) {
            widgetList.setCollapsed(true);
        }
        inspectorCollapsed = !inspectorCollapsed;
        draggingPanel = false;
        draggingScroll = false;
    }

    private int inspectorTop() {
        return panelY + HEADER_HEIGHT;
    }

    private int inspectorBottom() {
        return actionsTop() - 4;
    }

    private boolean insidePanel(double x, double y) {
        return x >= visiblePanelX() && x < visiblePanelX() + visiblePanelWidth()
                && y >= panelY && y < panelY + visiblePanelHeight();
    }

    private boolean insideInspector(double x, double y) {
        return insidePanel(x, y) && y >= inspectorTop() && y < inspectorBottom();
    }

    private void clampPanel() {
        panelX = Math.max(0, Math.min(width - panelWidth, panelX));
        panelY = Math.max(0, Math.min(height - panelHeight, panelY));
    }

    private void positionControls() {
        int left = panelX + 6;
        int half = (panelWidth - 16) / 2;
        int right = left + half + 4;
        int top = actionsTop();
        demoButton.setPosition(panelX + panelWidth - 46, panelY + 3);
        vanillaButton.setPosition(panelX + panelWidth - 114, panelY + 3);
        pointButton.setPosition(left, top);
        anchorButton.setPosition(right, top);
        parentButton.setPosition(left, top + BUTTON_STEP);
        hideButton.setPosition(right, top + BUTTON_STEP);
        resetButton.setPosition(left, top + BUTTON_STEP * 2);
        controls.get(7).setPosition(right, top + BUTTON_STEP * 2);
        controls.get(8).setPosition(left, top + BUTTON_STEP * 3);
        controls.get(9).setPosition(right, top + BUTTON_STEP * 3);
    }

    private void refreshFrameHandles() {
        frameHandles = manager.getHandles();
        frameVanillaHandles = manager.getVanillaHandles();
        List<WidgetListPanel.Entry> entries = new ArrayList<>(frameHandles.size() + frameVanillaHandles.size());
        for (VanillaHudGeometry.Reference reference : frameVanillaHandles) {
            entries.add(new WidgetListPanel.Entry(reference.id(), reference.active(), true));
        }
        for (AnchorHudManager.HudHandle handle : frameHandles) {
            entries.add(new WidgetListPanel.Entry(handle.id(), handle.visible(), false));
        }
        frameEntries = List.copyOf(entries);
    }

    private void selectFromList(WidgetListPanel.Entry entry) {
        if (choosingParent && selected != null) {
            if (selected.equals(entry.id())) {
                status = "Choose another widget as parent";
                return;
            }
            if (manager.setPreviewParent(selected, entry.id())) {
                choosingParent = false;
                status = "Parent set to " + entry.id();
                inspectorScroll = 0;
            } else {
                status = "Invalid or circular parent: " + entry.id();
            }
            return;
        }
        selectWidget(null);
        if (entry.readOnly()) {
            inspectedVanilla = entry.id();
            status = "Vanilla reference (read-only)";
        } else {
            selectWidget(entry.id());
            status = "";
        }
        draggingWidget = false;
        dragOrigin = null;
        snapResult = null;
    }

    private void selectWidget(String id) {
        if (id == null || !id.equals(selected)) {
            selected = id;
            inspectedId = null;
            inspectedVanilla = null;
            sourceProperties = List.of();
            inspectorScroll = 0;
        }
    }

    private void saveAndClose() {
        if (manager.saveEditing()) {
            minecraft.setScreen(null);
        } else {
            status = "Save failed: " + manager.getLastSaveError();
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        manager.renderEditorPreview(graphics, partialTick);
        manager.renderScrollBars(graphics, mouseX, mouseY, partialTick);
        refreshFrameHandles();
        resetHitboxes.clear();
        vanillaParentHitboxes.clear();
        List<AnchorHudManager.HudHandle> handles = frameHandles;
        VanillaHudGeometry.Reference hoveredVanilla = clickedVanillaHandle(mouseX, mouseY);
        if (vanillaViewMode != 0) {
            for (VanillaHudGeometry.Reference reference : frameVanillaHandles) {
                if (vanillaViewMode == 1 && !reference.active()) {
                    continue;
                }
                AnchorLayout.Bounds bounds = reference.bounds();
                if (bounds.width() <= 0 || bounds.height() <= 0) {
                    continue;
                }
                int color = reference.active() ? VANILLA_COLOR : VANILLA_INACTIVE_COLOR;
                outline(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), color, 1);
                if (hoveredVanilla != null && hoveredVanilla.id().equals(reference.id())) {
                    String label = reference.id() + " (vanilla, read-only)";
                    int labelWidth = Math.min(font.width(label) + 8, width - 12);
                    int labelX = Math.max(2, Math.min(bounds.x(), width - labelWidth - 2));
                    int labelY = Math.max(1, Math.min(bounds.y() - 12, height - 13));
                    if (compact || !insidePanel(labelX, labelY)) {
                        graphics.fill(labelX, labelY, labelX + labelWidth, labelY + 11, 0xDF171A27);
                        graphics.drawString(font, font.plainSubstrByWidth(label, labelWidth - 6), labelX + 3, labelY + 1, color, false);
                    }
                }
            }
        }
        AnchorHudManager.HudHandle hovered = clickedHandle(mouseX, mouseY);
        AnchorHudManager.HudHandle active = null;
        for (AnchorHudManager.HudHandle handle : handles) {
            if (handle.id().equals(selected)) {
                active = handle;
            }
            var bounds = handle.bounds();
            if (bounds.width() <= 0 || bounds.height() <= 0) {
                continue;
            }
            boolean chosen = handle.id().equals(selected);
            int color = chosen ? SELECTED_COLOR : choosingParent ? PARENT_COLOR : handle.visible() ? NORMAL_COLOR : HIDDEN_COLOR;
            outline(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), color, chosen ? 2 : 1);
            if (chosen) {
                graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0x28FFC45B);
            }
            if (chosen || (hovered != null && hovered.id().equals(handle.id()))) {
                String label = handle.id() + (handle.visible() ? "" : " (hidden)");
                int labelWidth = Math.min(font.width(label) + 8, width - 12);
                int labelX = Math.max(2, Math.min(bounds.x(), width - labelWidth - 2));
                if (compact || !insidePanel(labelX, bounds.y() - 12)) {
                    graphics.fill(labelX, bounds.y() - 12, labelX + labelWidth, bounds.y(), 0xD0101720);
                    graphics.drawString(font, font.plainSubstrByWidth(label, labelWidth - 6), labelX + 3, bounds.y() - 11, color, false);
                }
            }
        }

        if (draggingWidget && snapResult != null) {
            Integer guideX = snapResult.guideX();
            Integer guideY = snapResult.guideY();
            if (guideX != null && guideX >= 0 && guideX < width) {
                graphics.fill(guideX, 0, guideX + 1, height, SNAP_GUIDE_COLOR);
            }
            if (guideY != null && guideY >= 0 && guideY < height) {
                graphics.fill(0, guideY, width, guideY + 1, SNAP_GUIDE_COLOR);
            }
        }

        if (selected != null && active == null) {
            selectWidget(null);
            choosingParent = false;
            draggingWidget = false;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 300.0F);
        try {
            if (!compact) {
                graphics.fill(visiblePanelX(), panelY, visiblePanelX() + visiblePanelWidth(), panelY + visiblePanelHeight(), 0xFF121C29);
                graphics.fill(visiblePanelX(), panelY, visiblePanelX() + visiblePanelWidth(), panelY + 20, 0xFF254057);
                graphics.drawString(font, inspectorCollapsed ? "Inspector" : "HUD Editor",
                        visiblePanelX() + 6, panelY + 6, 0xFFE5EEF5, false);
                graphics.drawString(font, inspectorCollapsed ? "[+]" : "[-]",
                        inspectorCollapsed ? visiblePanelX() + visiblePanelWidth() - 26 : panelX + panelWidth - 140,
                        panelY + 6, 0xFFE5EEF5, false);
                if (!inspectorCollapsed) {
                    String hint = choosingParent ? "Choose a parent, or click a listed vanilla ID" : "Snap: 6px | Alt: free | Vanilla: read-only";
                    drawScaled(graphics, hint, panelX + 6, panelY + 22, 0xFFAAC1D4);
                    renderInspector(graphics, active);
                    graphics.fill(panelX + 3, actionsTop() - 3, panelX + panelWidth - 3, actionsTop() - 2, 0x88698DAB);
                }
            }

            resetButton.active = selected != null && !choosingParent;
            hideButton.active = selected != null && !choosingParent;
            pointButton.active = selected != null && !choosingParent;
            anchorButton.active = selected != null && !choosingParent;
            parentButton.active = selected != null;
            hideButton.setMessage(Component.literal(active != null && active.hiddenByUser() ? "Show" : "Hide"));
            parentButton.setMessage(Component.literal(choosingParent ? "Cancel parent" : "Pick parent"));
            demoButton.setMessage(Component.literal(manager.isTestEnabled() ? "Demo ON" : "Demo"));
            vanillaButton.setMessage(Component.literal(switch (vanillaViewMode) {
                case 0 -> "IDs OFF";
                case 1 -> "IDs ON";
                default -> "IDs ALL";
            }));
            for (Button control : controls) {
                control.visible = !compact && !inspectorCollapsed;
            }
            super.render(graphics, mouseX, mouseY, partialTick);
            if (!compact) {
                widgetList.render(graphics, font, mouseX, mouseY, frameEntries,
                        selected != null ? selected : inspectedVanilla, choosingParent);
            }
        } finally {
            graphics.pose().popPose();
        }

        if (handles.isEmpty()) {
            graphics.drawCenteredString(font, "No HUD widgets. Enable Demo in the panel.", width / 2, height / 2, 0xFFFFD37C);
        }
    }

    private void renderInspector(GuiGraphics graphics, AnchorHudManager.HudHandle active) {
        int top = inspectorTop();
        int bottom = inspectorBottom();
        int visibleHeight = Math.max(1, bottom - top);
        List<InspectorLine> lines = inspectorLines(active);
        maxInspectorScroll = Math.max(0, lines.size() * INSPECTOR_LINE_HEIGHT - visibleHeight);
        inspectorScroll = Math.max(0, Math.min(maxInspectorScroll, inspectorScroll));
        if (bottom <= top) {
            return;
        }
        graphics.enableScissor(panelX + 4, top, panelX + panelWidth - 4, bottom);
        int y = top - inspectorScroll;
        for (InspectorLine line : lines) {
            if (y + INSPECTOR_LINE_HEIGHT > top && y < bottom) {
                graphics.pose().pushPose();
                graphics.pose().translate(panelX + 7, y + 1, 0);
                graphics.pose().scale(INSPECTOR_SCALE, INSPECTOR_SCALE, 1.0F);
                graphics.drawString(font, line.text(), 0, 0, line.color(), false);
                graphics.pose().popPose();
                if (line.vanillaParent() != null && y >= top && y + INSPECTOR_LINE_HEIGHT <= bottom) {
                    vanillaParentHitboxes.add(new VanillaParentHitbox(line.vanillaParent(),
                            panelX + 4, y, panelWidth - 8, INSPECTOR_LINE_HEIGHT));
                }
                if (line.reset() != null) {
                    int x = panelX + panelWidth - 21;
                    int buttonY = y;
                    int background = line.modified() ? 0xCC3D6273 : 0x8840505D;
                    int foreground = line.modified() ? 0xFFECF8FF : 0xFF80919E;
                    graphics.fill(x, buttonY, x + RESET_SIZE, buttonY + RESET_SIZE, background);
                    graphics.drawString(font, "R", x + 2, buttonY + 1, foreground, false);
                    if (line.modified() && buttonY >= top && buttonY + RESET_SIZE <= bottom) {
                        resetHitboxes.add(new ResetHitbox(line.reset(), x, buttonY));
                    }
                }
            }
            y += INSPECTOR_LINE_HEIGHT;
        }
        graphics.disableScissor();
        if (maxInspectorScroll > 0) {
            int trackX = panelX + panelWidth - 5;
            int trackHeight = bottom - top;
            int thumbHeight = Math.max(9, trackHeight * trackHeight / (trackHeight + maxInspectorScroll));
            int thumbTop = top + inspectorScroll * (trackHeight - thumbHeight) / maxInspectorScroll;
            graphics.fill(trackX, top, trackX + 2, bottom, 0x66465C6F);
            graphics.fill(trackX, thumbTop, trackX + 2, thumbTop + thumbHeight, 0xFFD5DDEB);
        }
    }

    private List<InspectorLine> inspectorLines(AnchorHudManager.HudHandle active) {
        List<InspectorLine> lines = new ArrayList<>();
        if (choosingParent) {
            appendLine(lines, "CHOOSE PARENT", 0xFF8DF4AA);
            appendLine(lines, "Click another widget", 0xFFE5EEF5);
            appendLine(lines, "Click empty world: screen", 0xFFE5EEF5);
            appendLine(lines, "Esc: cancel selection", 0xFFE5EEF5);
            appendLine(lines, "VANILLA ANCHORS (click one):", VANILLA_COLOR);
            for (VanillaHudGeometry.Reference reference : frameVanillaHandles) {
                String suffix = reference.active() ? "" : " [inactive]";
                String label = font.plainSubstrByWidth(reference.id() + suffix,
                        Math.max(40, (int) ((panelWidth - 30) / INSPECTOR_SCALE)));
                lines.add(new InspectorLine(Component.literal(label).getVisualOrderText(),
                        reference.active() ? VANILLA_COLOR : VANILLA_INACTIVE_COLOR, null, false, reference.id()));
            }
        }
        if (!status.isEmpty()) {
            appendLine(lines, status, 0xFFFFCF64);
        }
        if (active == null && inspectedVanilla != null) {
            appendLine(lines, "VANILLA HUD (READ ONLY)", VANILLA_COLOR);
            appendLine(lines, "id: " + inspectedVanilla, VANILLA_COLOR);
            for (VanillaHudGeometry.Reference reference : frameVanillaHandles) {
                if (reference.id().equals(inspectedVanilla)) {
                    AnchorLayout.Bounds bounds = reference.bounds();
                    appendLine(lines, "position: " + bounds.x() + ", " + bounds.y(), 0xFFE5EEF5);
                    appendLine(lines, "size: " + bounds.width() + " x " + bounds.height(), 0xFFE5EEF5);
                    appendLine(lines, "active: " + reference.active(), 0xFFE5EEF5);
                    break;
                }
            }
            appendLine(lines, "Use as relativeTo on your widgets", 0xFFE5EEF5);
            appendLine(lines, "Vanilla HUD remains unchanged", 0xFFE5EEF5);
        } else if (active == null) {
            appendLine(lines, "SELECT A WIDGET", 0xFFFFCF64);
            appendLine(lines, "Click any widget outline.", 0xFFE5EEF5);
            appendLine(lines, "Drag widgets to move.", 0xFFE5EEF5);
            appendLine(lines, "Auto-align: 6 px (Alt: bypass)", 0xFFE5EEF5);
            appendLine(lines, "Arrows: 1 px / Shift: 10 px", 0xFFE5EEF5);
            appendLine(lines, "Ctrl+S: save / Esc: cancel", 0xFFE5EEF5);
        } else {
            appendLine(lines, "CURRENT WIDGET", 0xFFFFCF64);
            appendLine(lines, "R at right: reset this field", 0xFF96AEBE);
            appendLine(lines, "id: " + active.id(), 0xFFE5EEF5);
            appendEditableLine(lines, "parent: " + active.relativeTo(), AnchorHudManager.EditableProperty.PARENT, active);
            appendEditableLine(lines, "point: " + active.point(), AnchorHudManager.EditableProperty.POINT, active);
            appendEditableLine(lines, "anchor: " + active.anchor(), AnchorHudManager.EditableProperty.ANCHOR, active);
            var bounds = active.bounds();
            appendLine(lines, "screen: " + bounds.x() + ", " + bounds.y(), 0xFFE5EEF5);
            appendLine(lines, "size: " + bounds.width() + " x " + bounds.height(), 0xFFE5EEF5);
            appendLine(lines, "Magnet: 6 px | hold Alt to bypass", 0xFF96AEBE);
            appendEditableLine(lines, "user offset: " + active.offset().x() + ", " + active.offset().y(), AnchorHudManager.EditableProperty.OFFSET, active);
            appendEditableLine(lines, "visible: " + active.visible(), AnchorHudManager.EditableProperty.VISIBILITY, active);
            if (!active.id().equals(inspectedId)) {
                JsonObject settings = manager.getWidgetSettings(active.id());
                sourceProperties = HudInspectorProperties.flatten(settings);
                inspectedId = active.id();
            }
            appendLine(lines, "SOURCE JSON", 0xFFFFCF64);
            for (String property : sourceProperties) {
                appendLine(lines, property, 0xFFB7CEDF);
            }
        }
        return lines;
    }

    private void appendLine(List<InspectorLine> lines, String value, int color) {
        appendFormattedLine(lines, value, color, null, false);
    }

    private void appendEditableLine(List<InspectorLine> lines, String value, AnchorHudManager.EditableProperty property, AnchorHudManager.HudHandle handle) {
        appendFormattedLine(lines, value, 0xFFE5EEF5, property, handle.isModified(property));
    }

    private void appendFormattedLine(List<InspectorLine> lines, String value, int color, AnchorHudManager.EditableProperty property, boolean modified) {
        int reserved = property == null ? 20 : 39;
        int availableWidth = Math.max(20, (int) ((panelWidth - reserved) / INSPECTOR_SCALE));
        boolean first = true;
        for (FormattedCharSequence part : font.split(Component.literal(value), availableWidth)) {
            lines.add(new InspectorLine(part, color, first ? property : null, modified, null));
            first = false;
        }
    }

    private void drawScaled(GuiGraphics graphics, String value, int x, int y, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(INSPECTOR_SCALE, INSPECTOR_SCALE, 1.0F);
        graphics.drawString(font, font.plainSubstrByWidth(value, (int) ((panelWidth - 12) / INSPECTOR_SCALE)), 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color, int thickness) {
        graphics.fill(x, y, x + width, y + thickness, color);
        graphics.fill(x, y + height - thickness, x + width, y + height, color);
        graphics.fill(x, y, x + thickness, y + height, color);
        graphics.fill(x + width - thickness, y, x + width, y + height, color);
    }

    private AnchorHudManager.HudHandle clickedHandle(double mouseX, double mouseY) {
        List<AnchorHudManager.HudHandle> handles = frameHandles;
        for (int i = handles.size() - 1; i >= 0; i--) {
            AnchorHudManager.HudHandle handle = handles.get(i);
            var bounds = handle.bounds();
            if (bounds.width() > 0 && bounds.height() > 0 && mouseX >= bounds.x() && mouseY >= bounds.y()
                    && mouseX < bounds.x() + bounds.width() && mouseY < bounds.y() + bounds.height()) {
                return handle;
            }
        }
        return null;
    }

    private VanillaHudGeometry.Reference clickedVanillaHandle(double mouseX, double mouseY) {
        if (vanillaViewMode == 0) {
            return null;
        }
        VanillaHudGeometry.Reference closest = null;
        long bestArea = Long.MAX_VALUE;
        for (VanillaHudGeometry.Reference reference : frameVanillaHandles) {
            if (vanillaViewMode == 1 && !reference.active()) {
                continue;
            }
            AnchorLayout.Bounds bounds = reference.bounds();
            long area = (long) bounds.width() * bounds.height();
            if (area == 0 || area > (long) width * height / 2) {
                continue;
            }
            if (mouseX >= bounds.x() && mouseX < bounds.x() + bounds.width()
                    && mouseY >= bounds.y() && mouseY < bounds.y() + bounds.height()
                    && area < bestArea) {
                bestArea = area;
                closest = reference;
            }
        }
        return closest;
    }

    private void scrollTo(double mouseY) {
        int top = inspectorTop();
        int available = Math.max(1, inspectorBottom() - top);
        int thumbHeight = Math.max(9, available * available / Math.max(1, available + maxInspectorScroll));
        int travel = Math.max(1, available - thumbHeight);
        inspectorScroll = Math.max(0, Math.min(maxInspectorScroll, (int) Math.round((mouseY - top - thumbHeight / 2.0) * maxInspectorScroll / travel)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!compact && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && insidePanel(mouseX, mouseY)
                && mouseY < panelY + 20
                && (inspectorCollapsed || (mouseX >= panelX + panelWidth - 144
                && mouseX < panelX + panelWidth - 115))) {
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
                    && widgetList.overlaps(panelX, panelY, panelX + panelWidth, panelY + panelHeight)) {
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
        if (!compact && insidePanel(mouseX, mouseY)) {
            if (choosingParent && selected != null && insideInspector(mouseX, mouseY)) {
                for (VanillaParentHitbox hitbox : vanillaParentHitboxes) {
                    if (hitbox.contains(mouseX, mouseY)) {
                        if (manager.setPreviewParent(selected, hitbox.id())) {
                            choosingParent = false;
                            status = "Parent set to " + hitbox.id();
                            inspectorScroll = 0;
                        } else {
                            status = "Invalid parent: " + hitbox.id();
                        }
                        return true;
                    }
                }
            }
            if (selected != null && insideInspector(mouseX, mouseY)) {
                for (ResetHitbox hitbox : resetHitboxes) {
                    if (hitbox.contains(mouseX, mouseY) && manager.resetPreviewProperty(selected, hitbox.property())) {
                        status = "Restored " + hitbox.property().name().toLowerCase(java.util.Locale.ROOT) + " to default";
                        return true;
                    }
                }
            }
            if (mouseY < panelY + 20) {
                draggingPanel = true;
                dragStartX = mouseX;
                dragStartY = mouseY;
                dragPanelX = panelX;
                dragPanelY = panelY;
            } else if (insideInspector(mouseX, mouseY) && mouseX >= panelX + panelWidth - 10) {
                draggingScroll = true;
                scrollTo(mouseY);
            }
            return true;
        }

        if (manager.mouseClickedScrollBar(mouseX, mouseY, button)) {
            draggingWidget = false;
            dragOrigin = null;
            snapResult = null;
            return true;
        }

        AnchorHudManager.HudHandle clicked = clickedHandle(mouseX, mouseY);
        VanillaHudGeometry.Reference vanilla = clicked == null ? clickedVanillaHandle(mouseX, mouseY) : null;
        if (choosingParent && selected != null) {
            String parent = clicked != null ? clicked.id() : vanilla != null ? vanilla.id() : "screen";
            if (selected.equals(parent)) {
                status = "Choose another widget or empty world";
                return true;
            }
            if (manager.setPreviewParent(selected, parent)) {
                choosingParent = false;
                status = "Parent set to " + parent;
            } else {
                status = "Invalid or circular parent: " + parent;
            }
            inspectorScroll = 0;
            return true;
        }
        if (clicked != null) {
            selectWidget(clicked.id());
            dragStartX = mouseX;
            dragStartY = mouseY;
            startOffsetX = clicked.offset().x();
            startOffsetY = clicked.offset().y();
            var bounds = clicked.bounds();
            dragOrigin = new HudSnap.Widget(clicked.id(), clicked.relativeTo(), bounds.x(), bounds.y(), bounds.width(), bounds.height(), clicked.visible());
            snapResult = null;
            draggingWidget = true;
            status = "";
            return true;
        }
        selectWidget(null);
        if (vanilla != null) {
            inspectedVanilla = vanilla.id();
            status = "Vanilla HUD anchor (read-only)";
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (manager.mouseDraggedScrollBar(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (widgetList.dragged(mouseX, mouseY, button)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingPanel) {
            panelX = dragPanelX + (int) Math.round(mouseX - dragStartX);
            panelY = dragPanelY + (int) Math.round(mouseY - dragStartY);
            clampPanel();
            positionControls();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingScroll) {
            scrollTo(mouseY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingWidget && selected != null && dragOrigin != null && !choosingParent) {
            int desiredX = dragOrigin.x() + (int) Math.round(mouseX - dragStartX);
            int desiredY = dragOrigin.y() + (int) Math.round(mouseY - dragStartY);
            if (hasAltDown()) {
                snapResult = new HudSnap.Result(desiredX, desiredY, null, null);
            } else {
                List<HudSnap.Widget> widgets = new ArrayList<>();
                for (AnchorHudManager.HudHandle handle : frameHandles) {
                    var bounds = handle.bounds();
                    widgets.add(new HudSnap.Widget(handle.id(), handle.relativeTo(), bounds.x(), bounds.y(), bounds.width(), bounds.height(), handle.visible()));
                }
                for (VanillaHudGeometry.Reference reference : frameVanillaHandles) {
                    AnchorLayout.Bounds bounds = reference.bounds();
                    widgets.add(new HudSnap.Widget(reference.id(), "screen", bounds.x(), bounds.y(), bounds.width(), bounds.height(), reference.active()));
                }
                snapResult = HudSnap.snap(dragOrigin, desiredX, desiredY, widgets, width, height);
            }
            double parentScale = manager.dragScale(selected);
            manager.setPreviewOffset(selected, startOffsetX + (int) Math.round((snapResult.x() - dragOrigin.x()) / parentScale),
                    startOffsetY + (int) Math.round((snapResult.y() - dragOrigin.y()) / parentScale));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (manager.mouseReleasedScrollBar(mouseX, mouseY, button)) {
            return true;
        }
        if (widgetList.released(button)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && (draggingWidget || draggingPanel || draggingScroll)) {
            draggingWidget = false;
            draggingPanel = false;
            draggingScroll = false;
            dragOrigin = null;
            snapResult = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!compact && widgetList.scroll(mouseX, mouseY, scrollY, frameEntries)) {
            return true;
        }
        if (!compact && insideInspector(mouseX, mouseY)) {
            inspectorScroll = Math.max(0, Math.min(maxInspectorScroll, inspectorScroll - (int) Math.round(scrollY * 23)));
            return true;
        }
        if (manager.scrollContentsAt(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyRegistry.EDIT_HUD.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            saveAndClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && choosingParent) {
            choosingParent = false;
            status = "Parent selection cancelled";
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            compact = !compact;
            draggingPanel = false;
            draggingScroll = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_S && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            saveAndClose();
            return true;
        }
        if (selected != null && !choosingParent) {
            if (keyCode == GLFW.GLFW_KEY_R) {
                manager.resetPreviewOffset(selected);
                return true;
            }
            int amount = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 10 : 1;
            int dx = 0;
            int dy = 0;
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> dx = -amount;
                case GLFW.GLFW_KEY_RIGHT -> dx = amount;
                case GLFW.GLFW_KEY_UP -> dy = -amount;
                case GLFW.GLFW_KEY_DOWN -> dy = amount;
                default -> {
                }
            }
            if (dx != 0 || dy != 0) {
                HudLayoutOffsets.Offset current = manager.getOffset(selected);
                manager.setPreviewOffset(selected, current.x() + dx, current.y() + dy);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        manager.cancelEditing();
        super.onClose();
    }

    @Override
    public void removed() {
        manager.cancelEditing();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
