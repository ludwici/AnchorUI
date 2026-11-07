package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonObject;
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
    private static final int PANEL_WIDTH = 218;
    private static final int BUTTON_HEIGHT = 17;
    private static final int BUTTON_STEP = 20;
    private static final int FOOTER_HEIGHT = 85;
    private static final int HEADER_HEIGHT = 34;
    private static final int INSPECTOR_LINE_HEIGHT = 11;
    private static final float INSPECTOR_SCALE = 0.83F;

    private record InspectorLine(FormattedCharSequence text, int color) {
    }

    private final AnchorHudManager manager = AnchorHudManager.INSTANCE;
    private final List<Button> controls = new ArrayList<>();
    private boolean editorStarted;
    private boolean compact;
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
    private int dragPanelX;
    private int dragPanelY;
    private Button resetButton;
    private Button hideButton;
    private Button pointButton;
    private Button anchorButton;
    private Button parentButton;
    private Button demoButton;

    public AnchorHudEditorScreen() {
        super(Component.literal("Anchor UI HUD Editor"));
    }

    @Override
    protected void init() {
        draggingWidget = false;
        draggingPanel = false;
        draggingScroll = false;
        controls.clear();
        if (!editorStarted) {
            manager.beginEditing();
            editorStarted = true;
            panelWidth = Math.min(PANEL_WIDTH, width - 12);
            panelHeight = Math.min(350, Math.max(128, height - 80));
            panelX = width - panelWidth - 8;
            panelY = 8;
        } else {
            panelWidth = Math.min(PANEL_WIDTH, width - 12);
            panelHeight = Math.min(350, Math.max(128, height - 80));
            clampPanel();
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

    private int inspectorTop() {
        return panelY + HEADER_HEIGHT;
    }

    private int inspectorBottom() {
        return actionsTop() - 4;
    }

    private boolean insidePanel(double x, double y) {
        return x >= panelX && x < panelX + panelWidth && y >= panelY && y < panelY + panelHeight;
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
        pointButton.setPosition(left, top);
        anchorButton.setPosition(right, top);
        parentButton.setPosition(left, top + BUTTON_STEP);
        hideButton.setPosition(right, top + BUTTON_STEP);
        resetButton.setPosition(left, top + BUTTON_STEP * 2);
        controls.get(6).setPosition(right, top + BUTTON_STEP * 2);
        controls.get(7).setPosition(left, top + BUTTON_STEP * 3);
        controls.get(8).setPosition(right, top + BUTTON_STEP * 3);
    }

    private void selectWidget(String id) {
        if (id == null || !id.equals(selected)) {
            selected = id;
            inspectedId = null;
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
        List<AnchorHudManager.HudHandle> handles = manager.getHandles();
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
            String label = handle.id() + (handle.visible() ? "" : " (hidden)");
            int labelX = Math.max(2, Math.min(bounds.x(), width - Math.min(font.width(label) + 8, width - 12) - 2));
            if (compact || !insidePanel(labelX, bounds.y() - 12)) {
                int labelWidth = Math.min(font.width(label) + 8, width - 12);
                graphics.fill(labelX, bounds.y() - 12, labelX + labelWidth, bounds.y(), 0xD0101720);
                graphics.drawString(font, font.plainSubstrByWidth(label, labelWidth - 6), labelX + 3, bounds.y() - 11, color, false);
            }
        }

        if (selected != null && active == null) {
            selectWidget(null);
            choosingParent = false;
            draggingWidget = false;
        }

        if (!compact) {
            graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE9121C29);
            graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 20, 0xEE254057);
            graphics.drawString(font, "Anchor UI - HUD", panelX + 6, panelY + 6, 0xFFE5EEF5, false);
            String hint = choosingParent ? "Select parent (empty = screen)" : "Drag header | Wheel: scroll | Tab: hide";
            drawScaled(graphics, hint, panelX + 6, panelY + 22, 0xFFAAC1D4);
            renderInspector(graphics, active);
            graphics.fill(panelX + 3, actionsTop() - 3, panelX + panelWidth - 3, actionsTop() - 2, 0x88698DAB);
        }

        resetButton.active = selected != null && !choosingParent;
        hideButton.active = selected != null && !choosingParent;
        pointButton.active = selected != null && !choosingParent;
        anchorButton.active = selected != null && !choosingParent;
        parentButton.active = selected != null;
        hideButton.setMessage(Component.literal(active != null && active.hiddenByUser() ? "Show" : "Hide"));
        parentButton.setMessage(Component.literal(choosingParent ? "Cancel parent" : "Pick parent"));
        demoButton.setMessage(Component.literal(manager.isTestEnabled() ? "Demo ON" : "Demo"));
        for (Button control : controls) {
            control.visible = !compact;
        }
        super.render(graphics, mouseX, mouseY, partialTick);

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
        }
        if (!status.isEmpty()) {
            appendLine(lines, status, 0xFFFFCF64);
        }
        if (active == null) {
            appendLine(lines, "SELECT A WIDGET", 0xFFFFCF64);
            appendLine(lines, "Click any widget outline.", 0xFFE5EEF5);
            appendLine(lines, "Drag widgets to move.", 0xFFE5EEF5);
            appendLine(lines, "Arrows: 1 px / Shift: 10 px", 0xFFE5EEF5);
            appendLine(lines, "Ctrl+S: save / Esc: cancel", 0xFFE5EEF5);
        } else {
            appendLine(lines, "CURRENT WIDGET", 0xFFFFCF64);
            appendLine(lines, "id: " + active.id(), 0xFFE5EEF5);
            appendLine(lines, "parent: " + active.relativeTo(), 0xFFE5EEF5);
            appendLine(lines, "point: " + active.point(), 0xFFE5EEF5);
            appendLine(lines, "anchor: " + active.anchor(), 0xFFE5EEF5);
            var bounds = active.bounds();
            appendLine(lines, "screen: " + bounds.x() + ", " + bounds.y(), 0xFFE5EEF5);
            appendLine(lines, "size: " + bounds.width() + " x " + bounds.height(), 0xFFE5EEF5);
            appendLine(lines, "user offset: " + active.offset().x() + ", " + active.offset().y(), 0xFFE5EEF5);
            appendLine(lines, "visible: " + active.visible(), 0xFFE5EEF5);
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
        int availableWidth = Math.max(20, (int) ((panelWidth - 20) / INSPECTOR_SCALE));
        for (FormattedCharSequence part : font.split(Component.literal(value), availableWidth)) {
            lines.add(new InspectorLine(part, color));
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
        List<AnchorHudManager.HudHandle> handles = manager.getHandles();
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

    private void scrollTo(double mouseY) {
        int top = inspectorTop();
        int available = Math.max(1, inspectorBottom() - top);
        int thumbHeight = Math.max(9, available * available / Math.max(1, available + maxInspectorScroll));
        int travel = Math.max(1, available - thumbHeight);
        inspectorScroll = Math.max(0, Math.min(maxInspectorScroll, (int) Math.round((mouseY - top - thumbHeight / 2.0) * maxInspectorScroll / travel)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!compact && insidePanel(mouseX, mouseY)) {
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

        AnchorHudManager.HudHandle clicked = clickedHandle(mouseX, mouseY);
        if (choosingParent && selected != null) {
            String parent = clicked == null ? "screen" : clicked.id();
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
            draggingWidget = true;
            status = "";
            return true;
        }
        selectWidget(null);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
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
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingWidget && selected != null && !choosingParent) {
            manager.setPreviewOffset(selected, startOffsetX + (int) Math.round(mouseX - dragStartX), startOffsetY + (int) Math.round(mouseY - dragStartY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && (draggingWidget || draggingPanel || draggingScroll)) {
            draggingWidget = false;
            draggingPanel = false;
            draggingScroll = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!compact && insideInspector(mouseX, mouseY)) {
            inspectorScroll = Math.max(0, Math.min(maxInspectorScroll, inspectorScroll - (int) Math.round(scrollY * 23)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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
