package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.layout.WidgetContainers;
import com.ludwici.anchorui.KeyRegistry;
import com.ludwici.anchorui.client.gui.editor.WidgetListPanel;
import com.ludwici.anchorui.client.gui.editor.LayoutInspectorControls;
import com.ludwici.anchorui.client.gui.editor.EditorDropdown;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.widget.WidgetEditorRegistry;
import com.ludwici.anchorui.client.gui.editor.AppearanceInspectorControls;
import com.ludwici.anchorui.client.gui.NativeContainerScrollPanel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
    private static final List<String> ANCHOR_CHOICES = java.util.Arrays.stream(ElementAnchor.values()).map(ElementAnchor::name).toList();

    private record InspectorLine(FormattedCharSequence text, int color, AnchorHudManager.EditableProperty reset,
                                 boolean modified, String vanillaParent, LayoutInspectorControls.Control layoutControl,
                                 AppearanceInspectorControls.Control appearanceControl) {
        private InspectorLine(FormattedCharSequence text, int color, AnchorHudManager.EditableProperty reset,
                              boolean modified, String vanillaParent) {
            this(text, color, reset, modified, vanillaParent, null, null);
        }

        private InspectorLine(FormattedCharSequence text, int color, AnchorHudManager.EditableProperty reset,
                              boolean modified, String vanillaParent, LayoutInspectorControls.Control layoutControl) {
            this(text, color, reset, modified, vanillaParent, layoutControl, null);
        }
    }

    private record AppearanceHitbox(AppearanceInspectorControls.Control control, int direction, int x, int y) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + 14 && mouseY >= y && mouseY < y + RESET_SIZE;
        }
    }

    private record LayoutHitbox(LayoutInspectorControls.Control control, int direction, int x, int y) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + 14 && mouseY >= y && mouseY < y + RESET_SIZE;
        }
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
    private final EditorDropdown editorDropdown = new EditorDropdown();
    private final List<ResetHitbox> resetHitboxes = new ArrayList<>();
    private final List<LayoutHitbox> layoutHitboxes = new ArrayList<>();
    private final List<AppearanceHitbox> appearanceHitboxes = new ArrayList<>();
    private EditBox appearanceInput;
    private Button appearanceApply;
    private Button appearanceCancel;
    private AppearanceInspectorControls.Control editingAppearance;
    private String editingAppearanceId;
    private final List<VanillaParentHitbox> vanillaParentHitboxes = new ArrayList<>();
    private int vanillaViewMode = 1;
    private String inspectedVanilla;
    private boolean editorStarted;
    private boolean compact;
    private boolean inspectorCollapsed;
    private boolean choosingParent;
    private boolean draggingWidget;
    private boolean draggingPanel;
    private NativeContainerScrollPanel inspectorScrollBar;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int maxInspectorScroll;
    private int inspectorScroll;
    private String selected;
    private String inspectedId;
    private List<String> sourceProperties = List.of();
    private boolean sourceExpanded;
    private int sourceHeaderIndex = -1;
    private int sourceHeaderY = -1;
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
        inspectorScrollBar = null;
        dragOrigin = null;
        snapResult = null;
        controls.clear();
        editorDropdown.close();
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

        pointButton = addControl(Button.builder(Component.literal("Point ▼"),
                button -> openAnchorDropdown(true)).bounds(left, actionsY, halfWidth, BUTTON_HEIGHT).build());
        anchorButton = addControl(Button.builder(Component.literal("Anchor ▼"),
                button -> openAnchorDropdown(false)).bounds(right, actionsY, halfWidth, BUTTON_HEIGHT).build());
        parentButton = addControl(Button.builder(Component.literal("Pick parent"), button -> {
            if (selected != null) {
                editorDropdown.close();
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
        createAppearanceInput();
    }

    private void openAnchorDropdown(boolean pointChoice) {
        if (selected == null) {
            return;
        }
        AnchorHudManager.HudHandle handle = null;
        for (AnchorHudManager.HudHandle entry : frameHandles) {
            if (entry.id().equals(selected)) {
                handle = entry;
                break;
            }
        }
        if (handle == null) {
            return;
        }
        String id = selected;
        Button source = pointChoice ? pointButton : anchorButton;
        String current = (pointChoice ? handle.point() : handle.anchor()).name();
        editorDropdown.open(source.getX(), source.getY(), source.getWidth(), source.getHeight(),
                width, height, ANCHOR_CHOICES, current, value -> {
                    if (!id.equals(selected)) {
                        return;
                    }
                    ElementAnchor chosen = ElementAnchor.fromString(value);
                    boolean changed = pointChoice ? manager.setPreviewPoint(id, chosen)
                            : manager.setPreviewAnchor(id, chosen);
                    status = changed ? "" : "Invalid anchor position";
                });
    }

    private void openLayoutDropdown(LayoutHitbox hitbox) {
        if (selected == null) {
            return;
        }
        String id = selected;
        LayoutInspectorControls.Control control = hitbox.control();
        editorDropdown.open(hitbox.x() - 106, hitbox.y(), 14, RESET_SIZE, width, height,
                LayoutInspectorControls.choices(control), control.value().replace("anchorui:", ""), value -> {
                    status = manager.setPreviewLayout(id, control, value) ? "" : "Invalid layout value or constraints";
                });
    }

    private void openAppearanceDropdown(AppearanceHitbox hitbox) {
        if (selected == null) {
            return;
        }
        String id = selected;
        AppearanceInspectorControls.Control control = hitbox.control();
        editorDropdown.open(hitbox.x() - 106, hitbox.y(), 14, RESET_SIZE, width, height,
                control.field().choices(), control.value(), value -> {
                    status = manager.setPreviewAppearance(id, control, value) ? "" : manager.getLastSaveError();
                });
    }

    private void chooseParentTarget(String id) {
        if (selected == null || selected.equals(id)) {
            status = "Choose another widget";
            return;
        }
        boolean success;
        if ("screen".equals(id)) {
            success = manager.movePreviewToRoot(selected);
        } else if (manager.isPreviewContainer(id)) {
            success = manager.movePreviewIntoContainer(selected, id);
        } else {
            success = manager.setPreviewParent(selected, id);
        }
        status = success ? ("screen".equals(id) ? "Moved to screen root"
                : manager.isPreviewContainer(id) ? "Moved into " + id
                : "Anchor target: " + id)
                : "Invalid parent or circular container relationship";
        if (success) {
            choosingParent = false;
        }
        resetInspectorScroll();
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
        editorDropdown.close();
        draggingPanel = false;
        inspectorScrollBar = null;
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
        entries.addAll(manager.getWidgetEntries());
        frameEntries = List.copyOf(entries);
    }

    private void selectFromList(WidgetListPanel.Entry entry) {
        if (choosingParent && selected != null) {
            chooseParentTarget(entry.id());
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
            editorDropdown.close();
            selected = id;
            inspectedId = null;
            inspectedVanilla = null;
            sourceProperties = List.of();
            sourceExpanded = false;
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
        layoutHitboxes.clear();
        appearanceHitboxes.clear();
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
                control.visible = !compact && !inspectorCollapsed && editingAppearance == null;
            }
            if (editingAppearance != null) {
                int modalX = appearanceInput.getX() - 8;
                int modalY = appearanceInput.getY() - 23;
                graphics.fill(modalX - 1, modalY - 1, modalX + appearanceInput.getWidth() + 17,
                        modalY + 86, 0xFF162332);
                graphics.drawString(font, font.plainSubstrByWidth(editingAppearance.field().label(),
                        appearanceInput.getWidth()), modalX + 8, modalY + 8, SELECTED_COLOR, false);
                if (!status.isEmpty()) {
                    graphics.drawString(font, font.plainSubstrByWidth(status, appearanceInput.getWidth()),
                            modalX + 8, modalY + 74, 0xFFFFA68A, false);
                }
            }
            super.render(graphics, mouseX, mouseY, partialTick);
            if (!compact) {
                widgetList.render(graphics, font, mouseX, mouseY, frameEntries,
                        selected != null ? selected : inspectedVanilla, choosingParent);
            }
            editorDropdown.render(graphics, font, mouseX, mouseY);
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
            inspectorScrollBar = null;
            return;
        }
        updateInspectorScrollBar(top, bottom);
        sourceHeaderY = sourceHeaderIndex < 0 ? -1 : top - inspectorScroll + sourceHeaderIndex * INSPECTOR_LINE_HEIGHT;
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
                if (line.layoutControl() != null) {
                    drawLayoutControl(graphics, line.layoutControl(), y, top, bottom);
                }
                if (line.appearanceControl() != null) {
                    drawAppearanceControl(graphics, line.appearanceControl(), y, top, bottom);
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
        if (inspectorScrollBar != null) {
            inspectorScrollBar.render(graphics, 0, 0, 0);
        }
    }

    private void drawLayoutControl(GuiGraphics graphics, LayoutInspectorControls.Control control,
                                   int lineY, int top, int bottom) {
        int first = panelX + panelWidth - 79;
        int next = panelX + panelWidth - 61;
        int reset = panelX + panelWidth - 24;
        boolean choice = !LayoutInspectorControls.choices(control).isEmpty();
        if (choice) {
            graphics.fill(next, lineY, next + 14, lineY + RESET_SIZE, 0xFF40566A);
            graphics.drawString(font, "▼", next + 2, lineY + 1, 0xFFE5EEF5, false);
        } else {
            graphics.fill(first, lineY, first + 14, lineY + RESET_SIZE, 0xFF40566A);
            graphics.fill(next, lineY, next + 14, lineY + RESET_SIZE, 0xFF40566A);
            graphics.drawString(font, "-", first + 4, lineY + 1, 0xFFE5EEF5, false);
            graphics.drawString(font, "+", next + 3, lineY + 1, 0xFFE5EEF5, false);
        }
        graphics.fill(reset, lineY, reset + 14, lineY + RESET_SIZE,
                control.modified() ? 0xFF3D6273 : 0xFF40505D);
        graphics.drawString(font, "R", reset + 3, lineY + 1,
                control.modified() ? 0xFFECF8FF : 0xFF80919E, false);
        if (lineY >= top && lineY + RESET_SIZE <= bottom) {
            if (!choice) {
                layoutHitboxes.add(new LayoutHitbox(control, -1, first, lineY));
            }
            layoutHitboxes.add(new LayoutHitbox(control, choice ? 2 : 1, next, lineY));
            if (control.modified()) {
                layoutHitboxes.add(new LayoutHitbox(control, 0, reset, lineY));
            }
        }
    }

    private void createAppearanceInput() {
        int w = Math.min(300, Math.max(100, width - 20));
        int x = (width - w) / 2;
        int y = Math.max(24, height / 2 - 38);
        appearanceInput = new EditBox(font, x + 8, y + 23, w - 16, 19, Component.literal("Appearance value"));
        appearanceInput.setMaxLength(4096);
        appearanceApply = Button.builder(Component.literal("Apply"), button -> commitAppearance())
                .bounds(x + 8, y + 48, (w - 24) / 2, 20).build();
        appearanceCancel = Button.builder(Component.literal("Cancel"), button -> closeAppearanceInput())
                .bounds(x + 16 + (w - 24) / 2, y + 48, (w - 24) / 2, 20).build();
        addRenderableWidget(appearanceInput);
        addRenderableWidget(appearanceApply);
        addRenderableWidget(appearanceCancel);
        closeAppearanceInput();
    }

    private void openAppearanceInput(String id, AppearanceInspectorControls.Control control) {
        status = "";
        editorDropdown.close();
        editingAppearance = control;
        editingAppearanceId = id;
        appearanceInput.setValue(control.value());
        appearanceInput.visible = true;
        appearanceApply.visible = true;
        appearanceCancel.visible = true;
        appearanceInput.setFocused(true);
        setFocused(appearanceInput);
    }

    private void closeAppearanceInput() {
        editingAppearance = null;
        editingAppearanceId = null;
        if (appearanceInput != null) {
            appearanceInput.visible = false;
            appearanceInput.setFocused(false);
        }
        if (appearanceApply != null) {
            appearanceApply.visible = false;
            appearanceCancel.visible = false;
        }
    }

    private void commitAppearance() {
        if (editingAppearance == null) {
            return;
        }
        if (manager.setPreviewAppearance(editingAppearanceId, editingAppearance, appearanceInput.getValue())) {
            status = "";
            closeAppearanceInput();
        } else {
            status = manager.getLastSaveError();
        }
    }

    private void drawAppearanceControl(GuiGraphics graphics, AppearanceInspectorControls.Control control,
                                       int lineY, int top, int bottom) {
        int left = panelX + panelWidth - 79;
        int next = panelX + panelWidth - 61;
        int reset = panelX + panelWidth - 24;
        boolean choice = control.field().kind() == WidgetEditorRegistry.Kind.CHOICE;
        if (control.textual() || choice) {
            graphics.fill(next, lineY, next + 14, lineY + RESET_SIZE, 0xFF40566A);
            graphics.drawString(font, choice ? "▼" : "E", next + 2, lineY + 1, 0xFFE5EEF5, false);
        } else {
            graphics.fill(left, lineY, left + 14, lineY + RESET_SIZE, 0xFF40566A);
            graphics.fill(next, lineY, next + 14, lineY + RESET_SIZE, 0xFF40566A);
            graphics.drawString(font, "-", left + 4, lineY + 1, 0xFFE5EEF5, false);
            graphics.drawString(font, "+", next + 3, lineY + 1, 0xFFE5EEF5, false);
        }
        graphics.fill(reset, lineY, reset + 14, lineY + RESET_SIZE,
                control.modified() ? 0xFF3D6273 : 0xFF40505D);
        graphics.drawString(font, "R", reset + 3, lineY + 1,
                control.modified() ? 0xFFECF8FF : 0xFF80919E, false);
        if (lineY >= top && lineY + RESET_SIZE <= bottom) {
            if (!control.textual() && !choice) {
                appearanceHitboxes.add(new AppearanceHitbox(control, -1, left, lineY));
            }
            appearanceHitboxes.add(new AppearanceHitbox(control, choice ? 3 : control.textual() ? 2 : 1, next, lineY));
            if (control.modified()) {
                appearanceHitboxes.add(new AppearanceHitbox(control, 0, reset, lineY));
            }
        }
    }

    private List<InspectorLine> inspectorLines(AnchorHudManager.HudHandle active) {
        List<InspectorLine> lines = new ArrayList<>();
        sourceHeaderIndex = -1;
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
            String parentId = manager.getStructuralParent(active.id());
            if ("(none)".equals(parentId)) {
                appendEditableLine(lines, "parent: " + active.relativeTo(), AnchorHudManager.EditableProperty.PARENT, active);
            } else {
                appendLine(lines, "parent: " + parentId, 0xFFE5EEF5);
            }
            appendEditableLine(lines, "point: " + active.point(), AnchorHudManager.EditableProperty.POINT, active);
            appendEditableLine(lines, "anchor: " + active.anchor(), AnchorHudManager.EditableProperty.ANCHOR, active);
            var bounds = active.bounds();
            appendLine(lines, "screen: " + bounds.x() + ", " + bounds.y(), 0xFFE5EEF5);
            appendLine(lines, "size: " + bounds.width() + " x " + bounds.height(), 0xFFE5EEF5);
            appendLine(lines, "Magnet: 6 px | hold Alt to bypass", 0xFF96AEBE);
            appendEditableLine(lines, "user offset: " + active.offset().x() + ", " + active.offset().y(), AnchorHudManager.EditableProperty.OFFSET, active);
            appendEditableLine(lines, "visible: " + active.visible(), AnchorHudManager.EditableProperty.VISIBILITY, active);
            List<LayoutInspectorControls.Control> layoutControls = manager.getLayoutControls(active.id());
            if (!layoutControls.isEmpty()) {
                appendLine(lines, "COMPONENT / LAYOUT", 0xFFFFCF64);
                appendLine(lines, "- / + adjust | R reset", 0xFF96AEBE);
                for (LayoutInspectorControls.Control control : layoutControls) {
                    String label = control.label() + ": " + control.value();
                    int available = Math.max(38, (int) ((panelWidth - 98) / INSPECTOR_SCALE));
                    String clipped = font.plainSubstrByWidth(label, available);
                    lines.add(new InspectorLine(Component.literal(clipped).getVisualOrderText(),
                            control.modified() ? 0xFF92DFCE : 0xFFE5EEF5, null,
                            control.modified(), null, control));
                }
            }
            List<AppearanceInspectorControls.Control> appearanceControls = manager.getAppearanceControls(active.id());
            if (!appearanceControls.isEmpty()) {
                appendLine(lines, "VISUAL / CONTENT", 0xFFFFCF64);
                for (AppearanceInspectorControls.Control control : appearanceControls) {
                    String label = control.field().label() + ": " + control.value();
                    int available = Math.max(38, (int) ((panelWidth - 98) / INSPECTOR_SCALE));
                    String clipped = font.plainSubstrByWidth(label, available);
                    lines.add(new InspectorLine(Component.literal(clipped).getVisualOrderText(),
                            control.modified() ? 0xFF92DFCE : 0xFFE5EEF5,
                            null, control.modified(), null, null, control));
                }
            }
            sourceHeaderIndex = lines.size();
            appendLine(lines, "SOURCE JSON " + (sourceExpanded ? "[-]" : "[+]") + " (read only)", 0xFFFFCF64);
            if (sourceExpanded) {
                if (!active.id().equals(inspectedId)) {
                    JsonObject settings = manager.getWidgetSettings(active.id());
                    String widgetParent = manager.getStructuralParent(active.id());
                    sourceProperties = HudInspectorProperties.flatten(settings,
                            "(none)".equals(widgetParent) ? null : widgetParent);
                    inspectedId = active.id();
                }
                for (String property : sourceProperties) {
                    appendLine(lines, property, 0xFFB7CEDF);
                }
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

    private AnchorHudManager.HudHandle clickedParentPanel(double x, double y) {
        for (int index = frameHandles.size() - 1; index >= 0; index--) {
            AnchorHudManager.HudHandle handle = frameHandles.get(index);
            if (!manager.isPreviewContainer(handle.id()) || handle.id().equals(selected)) {
                continue;
            }
            AnchorLayout.Bounds bounds = handle.bounds();
            if (bounds.width() > 0 && bounds.height() > 0
                    && x >= bounds.x() && x < bounds.x() + bounds.width()
                    && y >= bounds.y() && y < bounds.y() + bounds.height()) {
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

    private void updateInspectorScrollBar(int top, int bottom) {
        if (maxInspectorScroll <= 0 || bottom - top < 34 || panelWidth < 20) {
            inspectorScrollBar = null;
            inspectorScroll = 0;
            return;
        }
        AnchorLayout.Bounds viewport = new AnchorLayout.Bounds(panelX + 4, top, panelWidth - 7, bottom - top);
        if (inspectorScrollBar == null || !inspectorScrollBar.matches(viewport, maxInspectorScroll, 23, 1.0, NativeContainerScrollPanel.HUD_INSPECTOR)) {
            inspectorScrollBar = new NativeContainerScrollPanel(minecraft, viewport, maxInspectorScroll, 23, 1.0,
                    inspectorScroll, NativeContainerScrollPanel.HUD_INSPECTOR);
        }
        inspectorScroll = inspectorScrollBar.logicalOffset();
    }

    private void resetInspectorScroll() {
        inspectorScroll = 0;
        if (inspectorScrollBar != null) {
            inspectorScrollBar.setLogicalOffset(0);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editorDropdown.isOpen()) {
            return editorDropdown.mouseClicked(mouseX, mouseY, button);
        }
        if (editingAppearance != null) {
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
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
                            resetInspectorScroll();
                        } else {
                            status = "Invalid parent: " + hitbox.id();
                        }
                        return true;
                    }
                }
            }
            if (selected != null && insideInspector(mouseX, mouseY)) {
                if (sourceHeaderY >= inspectorTop() && sourceHeaderY + INSPECTOR_LINE_HEIGHT <= inspectorBottom()
                        && mouseY >= sourceHeaderY && mouseY < sourceHeaderY + INSPECTOR_LINE_HEIGHT) {
                    sourceExpanded = !sourceExpanded;
                    return true;
                }
                for (AppearanceHitbox hitbox : appearanceHitboxes) {
                    if (hitbox.contains(mouseX, mouseY)) {
                        if (hitbox.direction() == 0) {
                            status = manager.resetPreviewAppearance(selected, hitbox.control())
                                    ? "" : manager.getLastSaveError();
                        } else if (hitbox.direction() == 2) {
                            openAppearanceInput(selected, hitbox.control());
                        } else if (hitbox.direction() == 3) {
                            openAppearanceDropdown(hitbox);
                        } else {
                            try {
                                String value = AppearanceInspectorControls.stepped(hitbox.control(), hitbox.direction()).getAsString();
                                status = manager.setPreviewAppearance(selected, hitbox.control(), value)
                                        ? "" : manager.getLastSaveError();
                            } catch (RuntimeException exception) {
                                status = exception.getMessage();
                            }
                        }
                        return true;
                    }
                }
                for (LayoutHitbox hitbox : layoutHitboxes) {
                    if (hitbox.contains(mouseX, mouseY)) {
                        if (hitbox.direction() == 2) {
                            openLayoutDropdown(hitbox);
                            return true;
                        }
                        boolean changed = hitbox.direction() == 0
                                ? manager.resetPreviewLayout(selected, hitbox.control())
                                : manager.adjustPreviewLayout(selected, hitbox.control(), hitbox.direction());
                        status = changed ? "" : "Invalid layout value or constraints";
                        return true;
                    }
                }
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
            } else if (insideInspector(mouseX, mouseY) && inspectorScrollBar != null) {
                inspectorScrollBar.mouseClicked(mouseX, mouseY, button);
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
            AnchorHudManager.HudHandle panel = clickedParentPanel(mouseX, mouseY);
            VanillaHudGeometry.Reference targetReference = panel == null
                    ? clickedVanillaHandle(mouseX, mouseY) : null;
            chooseParentTarget(panel != null ? panel.id()
                    : targetReference != null ? targetReference.id() : "screen");
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
        if (inspectorScrollBar != null && inspectorScrollBar.isDraggingBar()) {
            inspectorScrollBar.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            inspectorScroll = inspectorScrollBar.logicalOffset();
            return true;
        }
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
        if (inspectorScrollBar != null && inspectorScrollBar.isDraggingBar()) {
            inspectorScrollBar.mouseReleased(mouseX, mouseY, button);
            inspectorScroll = inspectorScrollBar.logicalOffset();
            return true;
        }
        if (manager.mouseReleasedScrollBar(mouseX, mouseY, button)) {
            return true;
        }
        if (widgetList.released(button)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && (draggingWidget || draggingPanel)) {
            draggingWidget = false;
            draggingPanel = false;
            dragOrigin = null;
            snapResult = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (editorDropdown.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        if (!compact && widgetList.scroll(mouseX, mouseY, scrollY, frameEntries)) {
            return true;
        }
        if (!compact && insideInspector(mouseX, mouseY)) {
            if (inspectorScrollBar != null) {
                inspectorScrollBar.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
                inspectorScroll = inspectorScrollBar.logicalOffset();
            }
            return true;
        }
        if (manager.scrollContentsAt(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editorDropdown.keyPressed(keyCode)) {
            return true;
        }
        if (editingAppearance != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeAppearanceInput();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commitAppearance();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
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
            editorDropdown.close();
            compact = !compact;
            draggingPanel = false;
            inspectorScrollBar = null;
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
