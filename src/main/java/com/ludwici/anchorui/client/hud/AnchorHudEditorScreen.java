package com.ludwici.anchorui.client.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class AnchorHudEditorScreen extends Screen {
    private static final int SELECTED_COLOR = 0xFFFFCF64;
    private static final int NORMAL_COLOR = 0xFF69D7D9;
    private static final int HIDDEN_COLOR = 0xFF9E9E9E;
    private static final int PARENT_COLOR = 0xFF8DF4AA;
    private static final int PANEL_TOP = 4;
    private static final int PANEL_HEIGHT = 134;

    private final AnchorHudManager manager = AnchorHudManager.INSTANCE;
    private final List<Button> controls = new ArrayList<>();
    private boolean editorStarted;
    private boolean compact;
    private boolean choosingParent;
    private int panelLeft;
    private int panelWidth;
    private String selected;
    private String status = "";
    private boolean dragging;
    private double dragStartX;
    private double dragStartY;
    private int startOffsetX;
    private int startOffsetY;
    private Button resetSelectedButton;
    private Button visibilityButton;
    private Button pointButton;
    private Button anchorButton;
    private Button parentButton;
    private Button demoButton;

    public AnchorHudEditorScreen() {
        super(Component.literal("Anchor UI HUD Editor"));
    }

    @Override
    protected void init() {
        dragging = false;
        controls.clear();
        if (!editorStarted) {
            manager.beginEditing();
            editorStarted = true;
        }

        panelWidth = Math.min(390, Math.max(1, width - 8));
        panelLeft = (width - panelWidth) / 2;
        int gap = 5;
        int width3 = Math.min(112, Math.max(65, (width - 26) / 3));
        int row3Width = width3 * 3 + gap * 2;
        int row3X = (width - row3Width) / 2;

        pointButton = addControl(Button.builder(Component.literal("Point"), button -> {
            if (selected != null && manager.cyclePreviewPoint(selected)) {
                status = "Widget point changed; position recalculated";
            }
        }).bounds(row3X, PANEL_TOP + 56, width3, 20).build());
        anchorButton = addControl(Button.builder(Component.literal("Anchor"), button -> {
            if (selected != null && manager.cyclePreviewAnchor(selected)) {
                status = "Parent anchor changed; position recalculated";
            }
        }).bounds(row3X + width3 + gap, PANEL_TOP + 56, width3, 20).build());
        parentButton = addControl(Button.builder(Component.literal("Change parent"), button -> {
            if (selected != null) {
                choosingParent = !choosingParent;
                dragging = false;
                status = choosingParent ? "Click another widget, or empty space for screen" : "Parent selection cancelled";
            }
        }).bounds(row3X + (width3 + gap) * 2, PANEL_TOP + 56, width3, 20).build());

        int width4 = Math.min(90, Math.max(55, (width - 30) / 4));
        int row4Width = width4 * 4 + gap * 3;
        int row4X = (width - row4Width) / 2;
        resetSelectedButton = addControl(Button.builder(Component.literal("Reset"), button -> {
            if (selected != null) {
                manager.resetPreviewOffset(selected);
                status = "Restored widget defaults for " + selected;
            }
        }).bounds(row4X, PANEL_TOP + 80, width4, 20).build());
        visibilityButton = addControl(Button.builder(Component.literal("Hide"), button -> {
            if (selected != null) {
                manager.togglePreviewHidden(selected);
                status = "Changed visibility for " + selected;
            }
        }).bounds(row4X + width4 + gap, PANEL_TOP + 80, width4, 20).build());
        addControl(Button.builder(Component.literal("Reset all"), button -> {
            manager.resetAllPreviewOffsets();
            status = "Restored all HUD defaults";
        }).bounds(row4X + (width4 + gap) * 2, PANEL_TOP + 80, width4, 20).build());
        demoButton = addControl(Button.builder(Component.literal("Demo HUD"), button -> {
            manager.setTestEnabled(!manager.isTestEnabled());
            selected = null;
            choosingParent = false;
            status = manager.isTestEnabled() ? "Demo HUD enabled" : "Demo HUD disabled";
        }).bounds(row4X + (width4 + gap) * 3, PANEL_TOP + 80, width4, 20).build());

        int actionWidth = Math.min(115, Math.max(80, (width - 20) / 2 - 5));
        int actionsLeft = (width - actionWidth * 2 - 6) / 2;
        addControl(Button.builder(Component.literal("Save & close"), button -> saveAndClose()).bounds(actionsLeft, PANEL_TOP + 106, actionWidth, 20).build());
        addControl(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(actionsLeft + actionWidth + 6, PANEL_TOP + 106, actionWidth, 20).build());
    }

    private Button addControl(Button button) {
        controls.add(button);
        return addRenderableWidget(button);
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
        if (selected != null && handles.stream().noneMatch(handle -> handle.id().equals(selected))) {
            selected = null;
            choosingParent = false;
            dragging = false;
        }

        for (AnchorHudManager.HudHandle handle : handles) {
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
            int textWidth = Math.min(font.width(label) + 8, width - 12);
            int labelX = Math.max(2, Math.min(bounds.x(), width - textWidth - 2));
            if (compact || !insidePanel(labelX, bounds.y() - 12)) {
                graphics.fill(labelX, bounds.y() - 12, labelX + textWidth, bounds.y(), 0xD0101720);
                graphics.drawString(font, font.plainSubstrByWidth(label, textWidth - 6), labelX + 3, bounds.y() - 11, color, false);
            }
        }

        if (!compact) {
            graphics.fill(panelLeft, PANEL_TOP, panelLeft + panelWidth, PANEL_TOP + PANEL_HEIGHT, 0xE7121C29);
            drawPanelLine(graphics, "Anchor UI HUD Editor", PANEL_TOP + 5, 0xFFE6EDF5);
            drawPanelLine(graphics, choosingParent ? "Choose parent: click widget / empty world = screen / Esc = cancel"
                    : "Drag | Arrows: 1/10 px | Tab: hide controls | R: reset", PANEL_TOP + 17, 0xFFB3C9D9);
            String info = selected == null ? "Click a highlighted widget to select it" : details(selected, handles);
            drawPanelLine(graphics, info, PANEL_TOP + 29, 0xFFFFD37C);
            String summary = status.isEmpty() ? (manager.hasUnsavedChanges() ? "Unsaved changes - Save & close to keep them" : "Changes saved only with Save & close") : status;
            drawPanelLine(graphics, summary, PANEL_TOP + 41, manager.hasUnsavedChanges() ? 0xFFFFD37C : 0xFFD4E0EC);
        }

        if (handles.isEmpty()) {
            graphics.drawCenteredString(font, "No HUD widgets loaded. Enable Demo HUD above.", width / 2, height / 2, 0xFFFFD37C);
        }

        AnchorHudManager.HudHandle highlighted = null;
        for (AnchorHudManager.HudHandle handle : handles) {
            if (handle.id().equals(selected)) {
                highlighted = handle;
                break;
            }
        }
        resetSelectedButton.active = selected != null && !choosingParent;
        visibilityButton.active = selected != null && !choosingParent;
        pointButton.active = selected != null && !choosingParent;
        anchorButton.active = selected != null && !choosingParent;
        parentButton.active = selected != null;
        visibilityButton.setMessage(Component.literal(highlighted != null && highlighted.hiddenByUser() ? "Show" : "Hide"));
        pointButton.setMessage(Component.literal(highlighted == null ? "Point" : "Point: " + highlighted.point()));
        anchorButton.setMessage(Component.literal(highlighted == null ? "Anchor" : "Anchor: " + highlighted.anchor()));
        parentButton.setMessage(Component.literal(choosingParent ? "Cancel parent" : "Change parent"));
        demoButton.setMessage(Component.literal(manager.isTestEnabled() ? "Hide demo" : "Show demo"));
        for (Button control : controls) {
            control.visible = !compact;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawPanelLine(GuiGraphics graphics, String value, int y, int color) {
        graphics.drawString(font, font.plainSubstrByWidth(value, panelWidth - 16), panelLeft + 8, y, color, false);
    }

    private String details(String id, List<AnchorHudManager.HudHandle> handles) {
        for (AnchorHudManager.HudHandle handle : handles) {
            if (handle.id().equals(id)) {
                return id + " | parent: " + handle.relativeTo() + " | point: " + handle.point() + " -> " + handle.anchor() + " | shift: " + handle.offset().x() + ", " + handle.offset().y();
            }
        }
        return id;
    }

    private boolean insidePanel(double x, double y) {
        return x >= panelLeft && x < panelLeft + panelWidth && y >= PANEL_TOP && y < PANEL_TOP + PANEL_HEIGHT;
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!compact && insidePanel(mouseX, mouseY)) {
            return false;
        }

        AnchorHudManager.HudHandle clicked = clickedHandle(mouseX, mouseY);
        if (choosingParent && selected != null) {
            String parent = clicked == null ? "screen" : clicked.id();
            if (selected.equals(parent)) {
                status = "Choose a different parent or click empty world for screen";
                return true;
            }
            if (manager.setPreviewParent(selected, parent)) {
                choosingParent = false;
                status = "Parent set to " + parent + "; anchor position recalculated";
            } else {
                status = "Cannot use " + parent + " as parent (invalid or circular anchor)";
            }
            return true;
        }
        if (clicked != null) {
            selected = clicked.id();
            dragStartX = mouseX;
            dragStartY = mouseY;
            startOffsetX = clicked.offset().x();
            startOffsetY = clicked.offset().y();
            dragging = true;
            status = "";
            return true;
        }
        selected = null;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragging && selected != null && !choosingParent) {
            int x = startOffsetX + (int) Math.round(mouseX - dragStartX);
            int y = startOffsetY + (int) Math.round(mouseY - dragStartY);
            manager.setPreviewOffset(selected, x, y);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
