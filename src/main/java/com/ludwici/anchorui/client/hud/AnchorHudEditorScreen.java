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
    private static final int TOOLBAR_HEIGHT = 100;

    private final AnchorHudManager manager = AnchorHudManager.INSTANCE;
    private boolean editorStarted;
    private boolean compact;
    private final List<Button> controls = new ArrayList<>();
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
    private Button targetButton;
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
        int gap = 5;
        int width3 = Math.min(112, Math.max(65, (width - 26) / 3));
        int row3Width = width3 * 3 + gap * 2;
        int row3X = (width - row3Width) / 2;
        pointButton = addControl(Button.builder(Component.literal("Point"), button -> {
            if (selected != null && manager.cyclePreviewPoint(selected)) {
                status = "Changed widget attachment point";
            }
        }).bounds(row3X, height - 81, width3, 20).build());
        anchorButton = addControl(Button.builder(Component.literal("Anchor"), button -> {
            if (selected != null && manager.cyclePreviewTargetPoint(selected)) {
                status = "Changed target attachment point";
            }
        }).bounds(row3X + width3 + gap, height - 81, width3, 20).build());
        targetButton = addControl(Button.builder(Component.literal("Target"), button -> {
            if (selected != null) {
                status = manager.cyclePreviewTarget(selected) ? "Changed anchor target (position preserved)" : "No valid alternative anchor target";
            }
        }).bounds(row3X + (width3 + gap) * 2, height - 81, width3, 20).build());

        int width4 = Math.min(90, Math.max(55, (width - 30) / 4));
        int row4Width = width4 * 4 + gap * 3;
        int row4X = (width - row4Width) / 2;
        resetSelectedButton = addControl(Button.builder(Component.literal("Reset"), button -> {
            if (selected != null) {
                manager.resetPreviewOffset(selected);
                status = "Restored widget defaults for " + selected;
            }
        }).bounds(row4X, height - 54, width4, 20).build());
        visibilityButton = addControl(Button.builder(Component.literal("Hide"), button -> {
            if (selected != null) {
                manager.togglePreviewHidden(selected);
                status = "Changed visibility for " + selected;
            }
        }).bounds(row4X + width4 + gap, height - 54, width4, 20).build());
        addControl(Button.builder(Component.literal("Reset all"), button -> {
            manager.resetAllPreviewOffsets();
            status = "All HUD positions and visibility settings restored in preview";
        }).bounds(row4X + (width4 + gap) * 2, height - 54, width4, 20).build());
        demoButton = addControl(Button.builder(Component.literal("Demo HUD"), button -> {
            manager.setTestEnabled(!manager.isTestEnabled());
            selected = null;
            status = manager.isTestEnabled() ? "Demo HUD enabled for this session" : "Demo HUD disabled";
        }).bounds(row4X + (width4 + gap) * 3, height - 54, width4, 20).build());

        int actionWidth = Math.min(115, Math.max(80, (width - 20) / 2 - 5));
        int actionsLeft = (width - actionWidth * 2 - 6) / 2;
        addControl(Button.builder(Component.literal("Save & close"), button -> saveAndClose()).bounds(actionsLeft, height - 28, actionWidth, 20).build());
        addControl(Button.builder(Component.literal("Cancel"), button -> onClose()).bounds(actionsLeft + actionWidth + 6, height - 28, actionWidth, 20).build());
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        manager.renderEditorPreview(graphics, partialTick);

        List<AnchorHudManager.HudHandle> handles = manager.getHandles();
        if (selected != null && handles.stream().noneMatch(handle -> handle.id().equals(selected))) {
            selected = null;
            dragging = false;
        }

        for (AnchorHudManager.HudHandle handle : handles) {
            var bounds = handle.bounds();
            if (bounds.width() <= 0 || bounds.height() <= 0) {
                continue;
            }
            boolean chosen = handle.id().equals(selected);
            int color = chosen ? SELECTED_COLOR : handle.visible() ? NORMAL_COLOR : HIDDEN_COLOR;
            outline(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), color, chosen ? 2 : 1);
            if (chosen) {
                graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0x28FFC45B);
            }
            if (compact || (bounds.y() >= 58 && bounds.y() < height - TOOLBAR_HEIGHT - 15)) {
                String label = handle.id() + (handle.visible() ? "" : " (hidden)");
                int textWidth = Math.min(font.width(label) + 8, width - 12);
                int labelX = Math.max(2, Math.min(bounds.x(), width - textWidth - 2));
                graphics.fill(labelX, bounds.y() - 12, labelX + textWidth, bounds.y(), 0xD0101720);
                graphics.drawString(font, font.plainSubstrByWidth(label, textWidth - 6), labelX + 3, bounds.y() - 11, color, false);
            }
        }

        if (!compact) {
            graphics.fill(0, 0, width, 44, 0xDF111C29);
            graphics.drawString(font, "Anchor UI - HUD Editor", 10, 6, 0xFFE6EDF5, false);
            graphics.drawString(font, font.plainSubstrByWidth("Drag | Arrows: 1/10 px | Tab: hide controls | R: reset", width - 20), 10, 19, 0xFFB3C9D9, false);
            String info = selected == null ? "Click a highlighted widget to select it" : details(selected, handles);
            graphics.drawString(font, font.plainSubstrByWidth(info, width - 20), 10, 31, 0xFFFFD37C, false);

            graphics.fill(0, height - TOOLBAR_HEIGHT, width, height, 0xE7121C29);
            String summary = status.isEmpty() ? (manager.hasUnsavedChanges() ? "Unsaved changes - Save & close to keep them" : "Changes are saved only when you press Save & close") : status;
            graphics.drawString(font, font.plainSubstrByWidth(summary, width - 20), 8, height - TOOLBAR_HEIGHT + 2, manager.hasUnsavedChanges() ? 0xFFFFD37C : 0xFFD4E0EC, false);
        }

        if (handles.isEmpty()) {
            graphics.drawCenteredString(font, "No HUD widgets loaded. Enable Demo HUD below.", width / 2, height / 2, 0xFFFFD37C);
        }

        resetSelectedButton.active = selected != null;
        visibilityButton.active = selected != null;
        pointButton.active = selected != null;
        anchorButton.active = selected != null;
        targetButton.active = selected != null;
        AnchorHudManager.HudHandle highlighted = null;
        for (AnchorHudManager.HudHandle handle : handles) {
            if (handle.id().equals(selected)) {
                highlighted = handle;
                break;
            }
        }
        visibilityButton.setMessage(Component.literal(highlighted != null && highlighted.hiddenByUser() ? "Show" : "Hide"));
        pointButton.setMessage(Component.literal(highlighted == null ? "Point" : "Point: " + highlighted.point()));
        anchorButton.setMessage(Component.literal(highlighted == null ? "Anchor" : "Anchor: " + highlighted.anchor()));
        targetButton.setMessage(Component.literal("Change target"));
        demoButton.setMessage(Component.literal(manager.isTestEnabled() ? "Hide demo" : "Show demo"));
        for (Button control : controls) {
            control.visible = !compact;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String details(String id, List<AnchorHudManager.HudHandle> handles) {
        for (AnchorHudManager.HudHandle handle : handles) {
            if (handle.id().equals(id)) {
                return id + " | target: " + handle.relativeTo() + " | point: " + handle.point() + " -> " + handle.anchor() + " | shift: " + handle.offset().x() + ", " + handle.offset().y();
            }
        }
        return id;
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color, int thickness) {
        graphics.fill(x, y, x + width, y + thickness, color);
        graphics.fill(x, y + height - thickness, x + width, y + height, color);
        graphics.fill(x, y, x + thickness, y + height, color);
        graphics.fill(x + width - thickness, y, x + width, y + height, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!compact && (mouseY < 44 || mouseY >= height - TOOLBAR_HEIGHT)) {
            return false;
        }

        List<AnchorHudManager.HudHandle> handles = manager.getHandles();
        for (int i = handles.size() - 1; i >= 0; i--) {
            AnchorHudManager.HudHandle handle = handles.get(i);
            var bounds = handle.bounds();
            if (mouseX >= bounds.x() && mouseY >= bounds.y() && mouseX < bounds.x() + bounds.width() && mouseY < bounds.y() + bounds.height()) {
                selected = handle.id();
                dragStartX = mouseX;
                dragStartY = mouseY;
                startOffsetX = handle.offset().x();
                startOffsetY = handle.offset().y();
                dragging = true;
                status = "";
                return true;
            }
        }
        selected = null;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragging && selected != null) {
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
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            compact = !compact;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_S && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            saveAndClose();
            return true;
        }
        if (selected != null) {
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
