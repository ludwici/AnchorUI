package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.KeyRegistry;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import com.ludwici.anchorui.client.hud.HudSnap;
import com.ludwici.anchorui.client.gui.editor.WidgetListPanel;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class AnchorScreenEditorScreen extends Screen {
    private static final int WIDTH = 222;
    private static final int BACKGROUND = 0xEF18232F;
    private static final int OUTLINE = 0xFF67D4D8;
    private static final int SELECTED = 0xFFFFD16B;
    private static final int HIDDEN = 0xFF929BA7;
    private static final int GUIDES = 0xFF8DF0D3;

    private final ItemLoreScreen target;
    private final WidgetListPanel widgetList;
    private final List<Button> controls = new ArrayList<>();
    private ItemLoreScreen.EditorHandle selected;
    private Button hide;
    private Button parent;
    private Button reset;
    private Button point;
    private Button anchor;
    private boolean pickingParent;
    private boolean compact;
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
        if (panelLeft < 0) {
            panelLeft = Math.max(2, width - WIDTH - 8);
        }
        panelLeft = Math.max(0, Math.min(panelLeft, width - WIDTH));
        panelTop = Math.max(0, Math.min(panelTop, height - 245));
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

    private void positionControls() {
        for (int index = 0; index < controls.size(); index++) {
            controls.get(index).setPosition(panelX() + 7 + (index % 2) * 107, panelTop + 29 + (index / 2) * 24);
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
            button.visible = !compact;
        }
    }

    private void select(String id) {
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

    private List<WidgetListPanel.Entry> widgetEntries() {
        List<WidgetListPanel.Entry> entries = new ArrayList<>();
        for (ItemLoreScreen.EditorHandle handle : target.editorHandles()) {
            entries.add(new WidgetListPanel.Entry(handle.id(), handle.visible(), false));
        }
        return entries;
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
        List<ItemLoreScreen.EditorHandle> handles = target.editorHandles();
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        target.render(graphics, -1, -1, partialTick);
        for (ItemLoreScreen.EditorHandle handle : target.editorHandles()) {
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
        int x = panelX();
        int y = panelTop;
        int panelHeight = Math.min(height - y, 245);
        graphics.fill(x, y, x + WIDTH, y + panelHeight, BACKGROUND);
        graphics.fill(x, y, x + WIDTH, y + 1, OUTLINE);
        graphics.drawString(font, "Anchor UI - Screen Editor (drag)", x + 8, y + 9, 0xFFFFFFFF, false);
        if (selected == null) {
            graphics.drawString(font, "Click a widget to select it", x + 8, y + 135, 0xFFD9E7F1, false);
        } else {
            String id = selected.id();
            if (font.width(id) > WIDTH - 14) {
                id = font.plainSubstrByWidth(id, WIDTH - 20);
            }
            graphics.drawString(font, id, x + 8, y + 132, SELECTED, false);
            graphics.drawString(font, "X " + selected.bounds().x() + "  Y " + selected.bounds().y()
                    + "  " + selected.bounds().width() + " x " + selected.bounds().height(), x + 8, y + 147, 0xFFE1EAF1, false);
            graphics.drawString(font, "Offset: " + selected.offset().x() + ", " + selected.offset().y(), x + 8, y + 162, 0xFFE1EAF1, false);
            graphics.drawString(font, "Point: " + selected.point() + "  Anchor: " + selected.anchor(), x + 8, y + 177, 0xFFE1EAF1, false);
            String targetName = selected.relativeTo();
            if (font.width(targetName) > WIDTH - 70) {
                targetName = font.plainSubstrByWidth(targetName, WIDTH - 75);
            }
            graphics.drawString(font, "Parent: " + targetName, x + 8, y + 192, 0xFFE1EAF1, false);
        }
        graphics.drawString(font, "Drag: magnet 6px  |  Alt: free", x + 8, y + 210, 0xFFACD9D9, false);
        graphics.drawString(font, "Arrows: 1px  |  Shift: 10px", x + 8, y + 223, 0xFFACD9D9, false);
        if (!status.isEmpty()) {
            String message = font.plainSubstrByWidth(status, WIDTH - 18);
            graphics.drawString(font, message, x + 8, y + 236, 0xFFFFD16B, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        widgetList.render(graphics, font, mouseX, mouseY, widgetEntries(),
                selected == null ? null : selected.id(), pickingParent);
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
        if (!compact && widgetList.contains(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                WidgetListPanel.Entry entry = widgetList.entryAt(mouseX, mouseY, widgetEntries());
                if (entry != null) {
                    selectFromList(entry);
                }
            }
            widgetList.beginClick(mouseX, mouseY, button);
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        if (!compact && mouseX >= panelX() && mouseX <= panelX() + WIDTH
                && mouseY >= panelTop && mouseY <= panelTop + 245) {
            if (mouseY < panelTop + 27) {
                draggingPanel = true;
                panelStartX = mouseX;
                panelStartY = mouseY;
                panelDragLeft = panelLeft;
                panelDragTop = panelTop;
            }
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
        if (widgetList.dragged(mouseX, mouseY, button)) {
            return true;
        }
        if (draggingPanel && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            panelLeft = Math.max(0, Math.min(width - WIDTH, panelDragLeft + (int) Math.round(mouseX - panelStartX)));
            panelTop = Math.max(0, Math.min(height - 245, panelDragTop + (int) Math.round(mouseY - panelStartY)));
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
            snapped = HudSnap.snap(dragOrigin, desiredX, desiredY, targets);
        }
        double scale = target.editorDragScale(selected.id());
        int nextX = startOffset.x() + (int) Math.round((snapped.x() - dragOrigin.x()) / scale);
        int nextY = startOffset.y() + (int) Math.round((snapped.y() - dragOrigin.y()) / scale);
        target.editorOffset(selected.id(), nextX, nextY);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
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
        if (!compact && widgetList.scroll(mouseX, mouseY, scrollY, widgetEntries())) {
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
