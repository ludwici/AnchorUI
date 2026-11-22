package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.KeyRegistry;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.layout.WidgetContainers;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import com.ludwici.anchorui.client.hud.AnchorHudManager;
import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.hud.HudInspectorProperties;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.ludwici.anchorui.client.gui.editor.AnimationInspectorControls;
import com.ludwici.anchorui.client.hud.HudSnap;
import com.ludwici.anchorui.client.gui.NativeContainerScrollPanel;
import com.ludwici.anchorui.client.gui.editor.WidgetListPanel;
import com.ludwici.anchorui.client.gui.editor.LayoutInspectorControls;
import com.ludwici.anchorui.client.gui.editor.EditorDropdown;
import com.ludwici.anchorui.api.widget.WidgetEditorRegistry;
import com.ludwici.anchorui.client.gui.editor.AppearanceInspectorControls;
import com.ludwici.anchorui.client.gui.editor.InspectorSections;
import com.ludwici.anchorui.client.gui.editor.EditorInspectorTabs;
import com.ludwici.anchorui.client.gui.editor.EditorDiagnostics;
import com.ludwici.anchorui.client.gui.editor.AnchorInspectorRows;
import com.ludwici.anchorui.client.gui.editor.AnchorInspectorControls;
import com.ludwici.anchorui.client.gui.editor.EditorAnchorGuide;
import com.ludwici.anchorui.client.gui.editor.EditorInspectorCheckbox;
import com.ludwici.anchorui.client.gui.editor.EditorInspectorButton;
import com.ludwici.anchorui.client.gui.editor.EditorExitConfirmation;
import com.ludwici.anchorui.client.gui.editor.EditorColorPicker;
import com.ludwici.anchorui.client.gui.editor.EditorTextureBrowser;
import com.ludwici.anchorui.client.gui.editor.EditorWidgetCreation;
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
import java.util.HashMap;
import java.util.Map;

public final class AnchorScreenEditorScreen extends Screen {
    private static final int WIDTH = 234;
    private static final int BACKGROUND = 0xFF18232F;
    private static final int OUTLINE = 0xFF67D4D8;
    private static final int SELECTED = 0xFFFFD16B;
    private static final int HIDDEN = 0xFF929BA7;
    private static final int GUIDES = 0xFF8DF0D3;
    private static final int INSPECTOR_LINE_HEIGHT = 13;
    private static final int RESET_SIZE = 11;
    private static final float INSPECTOR_SCALE = 0.83F;
    private static final List<String> ANCHOR_CHOICES = java.util.Arrays.stream(ElementAnchor.values()).map(ElementAnchor::name).toList();

    private record InspectorLine(FormattedCharSequence text, int color, AnchorHudManager.EditableProperty property,
                                 boolean modified, LayoutInspectorControls.Control layoutControl,
                                 AppearanceInspectorControls.Control appearanceControl) {
        private InspectorLine(FormattedCharSequence text, int color, AnchorHudManager.EditableProperty property,
                              boolean modified) {
            this(text, color, property, modified, null, null);
        }

        private InspectorLine(FormattedCharSequence text, int color, AnchorHudManager.EditableProperty property,
                              boolean modified, LayoutInspectorControls.Control layoutControl) {
            this(text, color, property, modified, layoutControl, null);
        }
    }

    private record AppearanceHitbox(AppearanceInspectorControls.Control control, int direction, int x, int y) {
        private boolean contains(double mouseX, double mouseY) {
            return EditorInspectorButton.contains(mouseX, mouseY, x, y);
        }
    }

    private record LayoutHitbox(LayoutInspectorControls.Control control, int direction, int x, int y) {
        private boolean contains(double mouseX, double mouseY) {
            return EditorInspectorButton.contains(mouseX, mouseY, x, y);
        }
    }

    private record ResetHitbox(AnchorHudManager.EditableProperty property, int x, int y) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + RESET_SIZE && mouseY >= y && mouseY < y + RESET_SIZE;
        }
    }

    private record AnimationAction(String operation, int trackIndex) {
    }

    private record AnchorAction(int index, String field) {
    }

    private final Map<Integer, AnimationAction> animationActions = new HashMap<>();
    private final Map<Integer, AnchorAction> anchorActions = new HashMap<>();

    private final ItemLoreScreen target;
    private final WidgetListPanel widgetList;
    private List<ItemLoreScreen.EditorHandle> frameHandles = List.of();
    private List<WidgetListPanel.Entry> frameEntries = List.of();
    private final List<Button> controls = new ArrayList<>();
    private final EditorDropdown editorDropdown = new EditorDropdown();
    private final List<ResetHitbox> resetHitboxes = new ArrayList<>();
    private final List<LayoutHitbox> layoutHitboxes = new ArrayList<>();
    private final List<AppearanceHitbox> appearanceHitboxes = new ArrayList<>();
    private final InspectorSections inspectorSections = new InspectorSections();
    private final EditorInspectorTabs inspectorTabs = new EditorInspectorTabs();
    private final Map<Integer, String> moreActions = new HashMap<>();
    private boolean confirmResetAll;
    private EditBox appearanceInput;
    private Button appearanceApply;
    private Button appearanceCancel;
    private AppearanceInspectorControls.Control editingAppearance;
    private EditorColorPicker colorPickerDialog;
    private EditorTextureBrowser textureBrowser;
    private String editingAppearanceId;
    private AnchorAction editingAnchor;
    private String editingAnchorId;
    private List<String> sourceProperties = List.of();
    private String inspectedId;
    private int inspectorScroll;
    private int maxInspectorScroll;
    private NativeContainerScrollPanel inspectorScrollBar;
    private ItemLoreScreen.EditorHandle selected;
    private int focusedAnchorIndex = -1;
    private Button hide;
    private Button parent;
    private Button reset;
    private Button deleteWidget;
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
    private EditorExitConfirmation exitConfirmation;

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
        editorDropdown.close();
        exitConfirmation = new EditorExitConfirmation(width, height, this::discardAndClose, this::save);
        widgetList.resize(width, height);
        inspectorWidth = Math.min(WIDTH, Math.max(150, width - 116));
        if (panelLeft < 0) {
            panelLeft = Math.max(2, width - inspectorWidth - 8);
        }
        panelLeft = Math.max(0, Math.min(panelLeft, width - inspectorWidth));
        panelTop = Math.max(0, Math.min(panelTop, height - 155));
        if (!inspectorCollapsed && widgetList.overlaps(panelX(), panelTop, panelX() + inspectorWidth, panelTop + expandedPanelHeight())) {
            inspectorCollapsed = true;
        }
        int x = panelX();
        int y = panelTop;
        add(x + 7, y + 27, 100, "+ Add widget", button -> openCreateWidgetMenu());
        deleteWidget = add(x + 114, y + 27, 100, "Delete", button -> deleteCreatedWidget());
        hide = add(x + 7, y + 47, 65, "Hide", button -> toggleHidden());
        parent = add(x + 77, y + 47, 65, "Parent", button -> pickParent());
        reset = add(x + 147, y + 47, 65, "Reset", button -> reset());
        add(x + 7, y + 100, 100, "Save", button -> save());
        add(x + 114, y + 100, 100, "Cancel", button -> requestClose());
        positionControls();
        createAppearanceInput();
        updateControls();
    }

    private Button add(int x, int y, int width, String label, Button.OnPress action) {
        Button button = Button.builder(Component.literal(label), action).bounds(x, y, width, 17).build();
        controls.add(button);
        addRenderableWidget(button);
        return button;
    }

    private int panelX() {
        return Math.max(0, panelLeft);
    }

    private int expandedPanelHeight() {
        return Math.max(160, Math.min(440, height - panelTop - 8));
    }

    private int inspectorTop() {
        return panelTop + 101;
    }

    private int inspectorBottom() {
        return panelTop + expandedPanelHeight() - 42;
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
        if (inspectorCollapsed && widgetList.overlaps(panelX(), panelTop, panelX() + inspectorWidth, panelTop + expandedPanelHeight())) {
            widgetList.setCollapsed(true);
        }
        inspectorCollapsed = !inspectorCollapsed;
        editorDropdown.close();
        draggingPanel = false;
        updateControls();
    }

    private void positionControls() {
        int left = panelX() + 7;
        int usable = inspectorWidth - 14;
        int half = (usable - 6) / 2;
        int third = (usable - 8) / 3;
        controls.get(0).setWidth(half);
        controls.get(0).setPosition(left, panelTop + 27);
        controls.get(1).setWidth(usable - half - 6);
        controls.get(1).setPosition(left + half + 6, panelTop + 27);
        for (int index = 0; index < 3; index++) {
            Button action = controls.get(index + 2);
            action.setWidth(index == 2 ? usable - (third + 4) * 2 : third);
            action.setPosition(left + (third + 4) * index, panelTop + 47);
        }
        controls.get(5).setWidth(half);
        controls.get(5).setPosition(left, panelTop + expandedPanelHeight() - 22);
        controls.get(6).setWidth(usable - half - 6);
        controls.get(6).setPosition(left + half + 6, panelTop + expandedPanelHeight() - 22);
    }

    private void updateControls() {
        boolean hasSelection = selected != null;
        hide.active = hasSelection;
        reset.active = hasSelection;
        parent.active = hasSelection;
        deleteWidget.active = hasSelection && target.editorIsCreatedWidget(selected.id());
        hide.setMessage(Component.literal(hasSelection && selected.hiddenByUser() ? "Show" : "Hide"));
        parent.setMessage(Component.literal(pickingParent ? "Pick target..." : "Parent"));
        for (Button button : controls) {
            button.visible = !compact && !inspectorCollapsed;
        }
    }

    private void select(String id) {
        String previousId = selected == null ? null : selected.id();
        if (!java.util.Objects.equals(id, previousId)) {
            editorDropdown.close();
            inspectedId = null;
            sourceProperties = List.of();
            inspectorSections.collapse("source");
            focusedAnchorIndex = -1;
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
        frameEntries = target.editorWidgetEntries();
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
            chooseParentTarget(entry.id());
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

    private ItemLoreScreen.EditorHandle hitParentPanel(double x, double y) {
        for (int index = frameHandles.size() - 1; index >= 0; index--) {
            ItemLoreScreen.EditorHandle handle = frameHandles.get(index);
            if (!target.editorIsContainer(handle.id()) || selected != null && selected.id().equals(handle.id())) {
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

    private void requestClose() {
        if (target.hasUnsavedScreenChanges()) {
            exitConfirmation.show();
        } else {
            discardAndClose();
        }
    }

    private void discardAndClose() {
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

    private void openCreateWidgetMenu() {
        String parentId = selected != null && target.editorIsContainer(selected.id()) ? selected.id() : null;
        editorDropdown.open(panelX() + 7, panelTop + 101, inspectorWidth - 14, 20, width, height,
                EditorWidgetCreation.types(), "", type -> {
                    String id = target.editorCreateWidget(type);
                    if (id == null) {
                        status = target.screenSaveError();
                        return;
                    }
                    boolean moved = parentId == null || target.editorMoveIntoContainer(id, parentId);
                    select(id);
                    refreshFrameHandles();
                    status = moved ? "Created " + id : "Created at root; could not add to " + parentId;
                });
    }

    private void deleteCreatedWidget() {
        if (selected == null || !target.editorIsCreatedWidget(selected.id())) {
            return;
        }
        String id = selected.id();
        if (target.editorDeleteCreatedWidget(id)) {
            select(null);
            refreshFrameHandles();
            status = "Deleted " + id;
        } else {
            status = target.screenSaveError();
        }
    }

    private boolean updateAnchor(String id, List<PositionElement.Constraint> values) {
        boolean changed = target.editorSetAnchorConstraints(id, values);
        status = changed ? "" : target.editorLayoutFailure();
        refreshSelection();
        return changed;
    }

    private boolean clickAnchorAction(double mouseX, double mouseY) {
        int row = (int) Math.floor((mouseY - inspectorTop() + inspectorScroll) / INSPECTOR_LINE_HEIGHT);
        AnchorAction action = anchorActions.get(row);
        if (action == null || selected == null) {
            return false;
        }
        String id = selected.id();
        List<PositionElement.Constraint> values = target.editorAnchorConstraints(id);
        if (action.index() >= 0) {
            focusedAnchorIndex = action.index();
        }
        try {
            switch (action.field()) {
                case "add" -> {
                    if (updateAnchor(id, AnchorInspectorControls.add(values))) {
                        focusedAnchorIndex = values.size();
                        if (!inspectorSections.expanded("constraint:" + id + ":" + values.size())) {
                            inspectorSections.toggle("constraint:" + id + ":" + values.size());
                        }
                    }
                }
                case "reset" -> {
                    status = target.editorResetAnchorConstraints(id) ? "" : target.editorLayoutFailure();
                    if (status.isEmpty()) {
                        focusedAnchorIndex = -1;
                    }
                    refreshSelection();
                }
                case "delete" -> {
                    if (updateAnchor(id, AnchorInspectorControls.remove(values, action.index()))) {
                        focusedAnchorIndex = Math.min(action.index(), values.size() - 2);
                    }
                }
                case "point", "anchor", "relativeTo" -> openAnchorChoice(action, row, id, values);
                case "x", "y" -> {
                    int plus = panelX() + inspectorWidth - 25;
                    int minus = panelX() + inspectorWidth - 63;
                    if (mouseX >= minus && mouseX < minus + RESET_SIZE) {
                        updateAnchor(id, AnchorInspectorControls.step(values, action.index(), action.field(), hasShiftDown() ? -10 : -1));
                    } else if (mouseX >= plus && mouseX < plus + RESET_SIZE) {
                        updateAnchor(id, AnchorInspectorControls.step(values, action.index(), action.field(), hasShiftDown() ? 10 : 1));
                    } else {
                        openAnchorInput(id, action, values);
                    }
                }
                default -> {
                    return false;
                }
            }
        } catch (RuntimeException exception) {
            status = exception.getMessage();
        }
        return true;
    }

    private void openAnchorChoice(AnchorAction action, int row, String id, List<PositionElement.Constraint> values) {
        PositionElement.Constraint entry = values.get(action.index());
        List<String> options = new ArrayList<>();
        String current;
        if ("point".equals(action.field()) || "anchor".equals(action.field())) {
            options.addAll(ANCHOR_CHOICES);
            current = ("point".equals(action.field()) ? entry.point() : entry.anchor()).name();
        } else {
            options.add("screen");
            for (ItemLoreScreen.EditorHandle handle : target.editorHandles()) {
                if (!handle.id().equals(id) && !options.contains(handle.id())) {
                    options.add(handle.id());
                }
            }
            current = entry.relativeTo();
            if (!options.contains(current)) {
                options.add(current);
            }
        }
        int y = inspectorTop() - inspectorScroll + row * INSPECTOR_LINE_HEIGHT;
        editorDropdown.open(panelX() + 7, y, inspectorWidth - 24, INSPECTOR_LINE_HEIGHT, width, height, options, current, value -> {
            if (selected != null && id.equals(selected.id())) {
                try {
                    updateAnchor(id, AnchorInspectorControls.change(target.editorAnchorConstraints(id), action.index(), action.field(), value));
                } catch (RuntimeException exception) {
                    status = exception.getMessage();
                }
            }
        });
    }

    private void openAnchorInput(String id, AnchorAction action, List<PositionElement.Constraint> entries) {
        editorDropdown.close();
        editingAnchor = action;
        editingAnchorId = id;
        PositionElement.Constraint entry = entries.get(action.index());
        appearanceInput.setValue(Integer.toString("x".equals(action.field()) ? entry.x() : entry.y()));
        appearanceInput.visible = true;
        appearanceApply.visible = true;
        appearanceCancel.visible = true;
        appearanceInput.setFocused(true);
        setFocused(appearanceInput);
        updateControls();
    }

    private void openLayoutDropdown(LayoutHitbox hitbox) {
        if (selected == null) {
            return;
        }
        String id = selected.id();
        LayoutInspectorControls.Control control = hitbox.control();
        String current = control.value().replace("anchorui:", "");
        editorDropdown.open(hitbox.x() - 106, hitbox.y(), EditorInspectorButton.SIZE, RESET_SIZE, width, height,
                LayoutInspectorControls.choices(control), current, value -> {
                    status = target.editorSetLayout(id, control, value) ? "" : target.editorLayoutFailure();
                    refreshSelection();
                });
    }

    private void openAppearanceDropdown(AppearanceHitbox hitbox) {
        if (selected == null) {
            return;
        }
        String id = selected.id();
        AppearanceInspectorControls.Control control = hitbox.control();
        editorDropdown.open(hitbox.x() - 106, hitbox.y(), EditorInspectorButton.SIZE, RESET_SIZE, width, height,
                control.field().choices(), control.value(), value -> {
                    status = target.editorSetAppearance(id, control, value) ? "" : target.screenSaveError();
                    refreshSelection();
                });
    }

    private void pickParent() {
        editorDropdown.close();
        pickingParent = !pickingParent;
        status = pickingParent ? "Pick a container to add the widget, or empty space for screen" : "";
        updateControls();
    }

    private void chooseParentTarget(String id) {
        if (selected == null || selected.id().equals(id)) {
            status = "Choose a different widget";
            return;
        }
        boolean success;
        if ("screen".equals(id)) {
            success = target.editorMoveToRoot(selected.id());
        } else if (target.editorIsContainer(id)) {
            success = target.editorMoveIntoContainer(selected.id(), id);
        } else {
            success = target.editorAnchor(selected.id(), id, selected.point(), selected.anchor());
        }
        status = success ? ("screen".equals(id) ? "Moved to screen root"
                : target.editorIsContainer(id) ? "Moved into " + id
                : "Anchor target: " + id)
                : "Invalid parent or circular container relationship";
        if (success) {
            pickingParent = false;
        }
        refreshSelection();
        updateControls();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    boolean edits(ItemLoreScreen screen) {
        return target == screen;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        target.renderEditorPreview(graphics, partialTick);
        refreshFrameHandles();
        for (ItemLoreScreen.EditorHandle handle : frameHandles) {
            AnchorLayout.Bounds b = handle.bounds();
            int color = selected != null && selected.id().equals(handle.id()) ? SELECTED : handle.visible() ? OUTLINE : HIDDEN;
            outline(graphics, b.x(), b.y(), b.width(), b.height(), color);
            if (selected != null && selected.id().equals(handle.id())) {
                graphics.drawString(font, handle.id(), b.x(), b.y() - 10, color, false);
            }
        }
        renderAnchorGuide(graphics);
        if (dragging && snapped != null) {
            if (snapped.guideX() != null) {
                graphics.fill(snapped.guideX(), 0, snapped.guideX() + 1, height, GUIDES);
            }
            if (snapped.guideY() != null) {
                graphics.fill(0, snapped.guideY(), width, snapped.guideY() + 1, GUIDES);
            }
        }
        if (compact) {
            graphics.drawString(font, "Screen Editor (Tab: show panel, F8: exit)", 8, 7, SELECTED, false);
            exitConfirmation.render(graphics, font, mouseX, mouseY, partialTick);
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
            graphics.drawString(font, inspectorCollapsed ? "Inspector" : "Screen Editor", x + 8, y + 9, 0xFFFFFFFF, false);
            graphics.drawString(font, inspectorCollapsed ? "[+]" : "[-]", x + visiblePanelWidth() - 27, y + 9, 0xFFFFFFFF, false);
            if (!inspectorCollapsed) {
                inspectorTabs.render(graphics, font, x + 7, y + 77, inspectorWidth - 14);
                renderInspector(graphics);
                if (!status.isEmpty()) {
                    String message = font.plainSubstrByWidth(status, inspectorWidth - 18);
                    graphics.drawString(font, message, x + 8, y + expandedPanelHeight() - 35, 0xFFFFD16B, false);
                }
            }
            int backgroundMouseX = editingAppearance == null && editingAnchor == null && textureBrowser == null && !exitConfirmation.isOpen() ? mouseX : Integer.MIN_VALUE;
            int backgroundMouseY = editingAppearance == null && editingAnchor == null && textureBrowser == null && !exitConfirmation.isOpen() ? mouseY : Integer.MIN_VALUE;
            super.render(graphics, backgroundMouseX, backgroundMouseY, partialTick);
            widgetList.render(graphics, font, backgroundMouseX, backgroundMouseY, frameEntries, selected == null ? null : selected.id(), pickingParent);
            editorDropdown.render(graphics, font, mouseX, mouseY);
            if ((editingAppearance != null || editingAnchor != null) && colorPickerDialog == null && textureBrowser == null) {
                renderAppearanceModal(graphics, mouseX, mouseY, partialTick);
            }
            if (colorPickerDialog != null) {
                colorPickerDialog.render(graphics, font, mouseX, mouseY, partialTick, width, height, status);
            }
            if (textureBrowser != null) {
                textureBrowser.render(graphics, font, mouseX, mouseY, partialTick);
            }
            exitConfirmation.render(graphics, font, mouseX, mouseY, partialTick);
        } finally {
            graphics.pose().popPose();
        }
    }

    private void renderAnchorGuide(GuiGraphics graphics) {
        if (compact || inspectorCollapsed || selected == null || focusedAnchorIndex < 0 || !inspectorSections.expanded("anchors")) {
            return;
        }
        List<PositionElement.Constraint> anchors = target.editorAnchorConstraints(selected.id());
        if (focusedAnchorIndex >= anchors.size()) {
            focusedAnchorIndex = -1;
            return;
        }
        PositionElement.Constraint constraint = anchors.get(focusedAnchorIndex);
        AnchorLayout.Bounds targetBounds = "screen".equals(constraint.relativeTo()) ? new AnchorLayout.Bounds(0, 0, width, height) : null;
        if (targetBounds == null) {
            for (ItemLoreScreen.EditorHandle handle : frameHandles) {
                if (handle.id().equals(constraint.relativeTo())) {
                    targetBounds = handle.bounds();
                    break;
                }
            }
        }
        if (targetBounds != null) {
            EditorAnchorGuide.draw(graphics, font, EditorAnchorGuide.connect(selected.bounds(), targetBounds, constraint), width, height);
        }
    }

    private void renderInspector(GuiGraphics graphics) {
        int top = inspectorTop();
        int bottom = inspectorBottom();
        resetHitboxes.clear();
        layoutHitboxes.clear();
        appearanceHitboxes.clear();
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
            int row = 0;
            for (InspectorLine line : lines) {
                if (lineY + INSPECTOR_LINE_HEIGHT > top && lineY < bottom) {
                    boolean header = inspectorSections.isHeaderRow(row);
                    graphics.fill(panelX() + 5, lineY, panelX() + inspectorWidth - 7, lineY + INSPECTOR_LINE_HEIGHT - 1, header ? 0xFF284457 : row % 2 == 0 ? 0xFF1A2A37 : 0xFF1D2E3B);
                    if (line.modified() && !header) {
                        graphics.fill(panelX() + 5, lineY, panelX() + 7, lineY + INSPECTOR_LINE_HEIGHT - 1, 0xFF70CDB7);
                    }
                    graphics.pose().pushPose();
                    graphics.pose().translate(panelX() + 7, lineY + 1, 0);
                    graphics.pose().scale(INSPECTOR_SCALE, INSPECTOR_SCALE, 1.0F);
                    graphics.drawString(font, line.text(), 0, 0, line.color(), false);
                    graphics.pose().popPose();

                    AnchorAction anchorAction = anchorActions.get(row);
                    if (anchorAction != null && ("x".equals(anchorAction.field()) || "y".equals(anchorAction.field()))
                            && lineY >= top && lineY + RESET_SIZE <= bottom) {
                        EditorInspectorButton.draw(graphics, font, "-", panelX() + inspectorWidth - 63, lineY, 0xFF40566A, 0xFFE5EEF5);
                        EditorInspectorButton.draw(graphics, font, "+", panelX() + inspectorWidth - 25, lineY, 0xFF40566A, 0xFFE5EEF5);
                    }
                    if (line.layoutControl() != null) {
                        drawLayoutControl(graphics, line.layoutControl(), lineY, top, bottom);
                    }
                    if (line.appearanceControl() != null) {
                        drawAppearanceControl(graphics, line.appearanceControl(), lineY, top, bottom);
                    }
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
                row++;
            }
        } finally {
            graphics.disableScissor();
        }
        if (inspectorScrollBar != null) {
            inspectorScrollBar.render(graphics, 0, 0, 0);
        }
    }

    private void drawLayoutControl(GuiGraphics graphics, LayoutInspectorControls.Control control,
                                   int lineY, int top, int bottom) {
        int first = panelX() + inspectorWidth - 79;
        int next = panelX() + inspectorWidth - 61;
        int reset = panelX() + inspectorWidth - 24;
        boolean choice = !LayoutInspectorControls.choices(control).isEmpty();
        boolean checkbox = control.booleanField();
        if (checkbox) {
            EditorInspectorCheckbox.render(graphics, next, lineY, Boolean.parseBoolean(control.value()));
        } else if (choice) {
            EditorInspectorButton.draw(graphics, font, "▼", next, lineY, 0xFF40566A, 0xFFE5EEF5);
        } else {
            EditorInspectorButton.draw(graphics, font, "-", first, lineY, 0xFF40566A, 0xFFE5EEF5);
            EditorInspectorButton.draw(graphics, font, "+", next, lineY, 0xFF40566A, 0xFFE5EEF5);
        }
        EditorInspectorButton.draw(graphics, font, "R", reset, lineY, control.modified() ? 0xFF3D6273 : 0xFF40505D, control.modified() ? 0xFFECF8FF : 0xFF80919E);
        if (lineY >= top && lineY + RESET_SIZE <= bottom) {
            if (!choice && !checkbox) {
                layoutHitboxes.add(new LayoutHitbox(control, -1, first, lineY));
            }
            layoutHitboxes.add(new LayoutHitbox(control, choice ? 2 : 1, next, lineY));
            if (control.modified()) {
                layoutHitboxes.add(new LayoutHitbox(control, 0, reset, lineY));
            }
        }
    }

    private void renderAppearanceModal(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int modalX = appearanceInput.getX() - 8;
        int modalY = appearanceInput.getY() - 23;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        try {
            graphics.fill(modalX - 1, modalY - 1, modalX + appearanceInput.getWidth() + 17, modalY + 86, 0xFF718699);
            graphics.fill(modalX, modalY, modalX + appearanceInput.getWidth() + 16, modalY + 85, 0xFF162332);
            graphics.drawString(font, font.plainSubstrByWidth(editingAnchor == null ? editingAppearance.field().label() : "Anchor #" + (editingAnchor.index() + 1) + " " + editingAnchor.field().toUpperCase(), appearanceInput.getWidth()), modalX + 8, modalY + 8, SELECTED, false);
            if (!status.isEmpty()) {
                graphics.drawString(font, font.plainSubstrByWidth(status, appearanceInput.getWidth()), modalX + 8, modalY + 74, 0xFFFFA68A, false);
            }
            appearanceInput.render(graphics, mouseX, mouseY, partialTick);
            appearanceApply.render(graphics, mouseX, mouseY, partialTick);
            appearanceCancel.render(graphics, mouseX, mouseY, partialTick);
        } finally {
            graphics.pose().popPose();
        }
    }

    private boolean clickAppearanceModal(double mouseX, double mouseY, int button) {
        if (appearanceApply.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (appearanceCancel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        appearanceInput.mouseClicked(mouseX, mouseY, button);
        return true;
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
        if (control.field().kind() == WidgetEditorRegistry.Kind.COLOR) {
            colorPickerDialog = new EditorColorPicker(this, control.field().label(), control.value(), width, height,
                    value -> {
                        appearanceInput.setValue(value);
                        commitAppearance();
                    }, this::closeAppearanceInput);
            appearanceInput.visible = false;
            appearanceApply.visible = false;
            appearanceCancel.visible = false;
            updateControls();
            return;
        }
        if (control.field().kind() == WidgetEditorRegistry.Kind.TEXTURE) {
            textureBrowser = new EditorTextureBrowser(minecraft.getResourceManager(), control.value(), width, height,
                    value -> {
                        if (target.editorSetAppearance(id, control, value)) {
                            status = "";
                            closeAppearanceInput();
                            refreshSelection();
                        } else {
                            status = target.screenSaveError();
                            textureBrowser.showError(status);
                        }
                    }, this::closeAppearanceInput);
            appearanceInput.visible = false;
            appearanceApply.visible = false;
            appearanceCancel.visible = false;
            updateControls();
            return;
        }
        appearanceInput.visible = true;
        appearanceApply.visible = true;
        appearanceCancel.visible = true;
        appearanceInput.setFocused(true);
        setFocused(appearanceInput);
        updateControls();
    }

    private void closeAppearanceInput() {
        colorPickerDialog = null;
        textureBrowser = null;
        editingAppearance = null;
        editingAppearanceId = null;
        editingAnchor = null;
        editingAnchorId = null;
        if (appearanceInput != null) {
            appearanceInput.visible = false;
            appearanceInput.setFocused(false);
        }
        if (appearanceApply != null) {
            appearanceApply.visible = false;
            appearanceCancel.visible = false;
        }
        if (hide != null) {
            updateControls();
        }
    }

    private void commitAppearance() {
        if (editingAnchor != null) {
            try {
                if (updateAnchor(editingAnchorId, AnchorInspectorControls.change(target.editorAnchorConstraints(editingAnchorId), editingAnchor.index(), editingAnchor.field(), appearanceInput.getValue()))) {
                    closeAppearanceInput();
                }
            } catch (RuntimeException exception) {
                status = exception.getMessage();
            }
            return;
        }
        if (editingAppearance == null) {
            return;
        }
        if (target.editorSetAppearance(editingAppearanceId, editingAppearance, appearanceInput.getValue())) {
            status = "";
            closeAppearanceInput();
            refreshSelection();
        } else {
            status = target.screenSaveError();
        }
    }

    private void drawAppearanceControl(GuiGraphics graphics, AppearanceInspectorControls.Control control,
                                       int lineY, int top, int bottom) {
        int left = panelX() + inspectorWidth - 79;
        int next = panelX() + inspectorWidth - 61;
        int reset = panelX() + inspectorWidth - 24;
        boolean choice = control.field().kind() == WidgetEditorRegistry.Kind.CHOICE;
        boolean checkbox = control.booleanField();
        boolean colorField = control.field().kind() == WidgetEditorRegistry.Kind.COLOR;
        if (checkbox) {
            EditorInspectorCheckbox.render(graphics, next, lineY, Boolean.parseBoolean(control.value()));
        } else if (colorField) {
            try {
                EditorInspectorButton.drawColor(graphics, next, lineY, ColorPickerColor.parse(control.value()));
            } catch (IllegalArgumentException exception) {
                EditorInspectorButton.drawColor(graphics, next, lineY, 0xFF3D4550);
            }
        } else if (control.textual() || choice) {
            EditorInspectorButton.draw(graphics, font, choice ? "▼" : "E", next, lineY, 0xFF40566A, 0xFFE5EEF5);
        } else {
            EditorInspectorButton.draw(graphics, font, "-", left, lineY, 0xFF40566A, 0xFFE5EEF5);
            EditorInspectorButton.draw(graphics, font, "+", next, lineY, 0xFF40566A, 0xFFE5EEF5);
        }
        EditorInspectorButton.draw(graphics, font, "R", reset, lineY, control.modified() ? 0xFF3D6273 : 0xFF40505D, control.modified() ? 0xFFECF8FF : 0xFF80919E);
        if (lineY >= top && lineY + RESET_SIZE <= bottom) {
            if (!control.textual() && !choice && !checkbox) {
                appearanceHitboxes.add(new AppearanceHitbox(control, -1, left, lineY));
            }
            appearanceHitboxes.add(new AppearanceHitbox(control, choice ? 3 : control.textual() ? 2 : 1, next, lineY));
            if (control.modified()) {
                appearanceHitboxes.add(new AppearanceHitbox(control, 0, reset, lineY));
            }
        }
    }

    private List<InspectorLine> inspectorLines() {
        List<InspectorLine> lines = new ArrayList<>();
        inspectorSections.clearHeaders();
        animationActions.clear();
        anchorActions.clear();
        moreActions.clear();
        EditorDiagnostics.Summary diagnostics = EditorDiagnostics.summarize(target.getLayoutError(), target.getLayoutWarnings(), LoreLoader.INSTANCE.getResourceIssues().stream().filter(issue -> issue.appliesToScreen(target.editorResourceId())).toList());
        if (inspectorTabs.active(EditorInspectorTabs.Page.MORE) && !diagnostics.empty()) {
            appendSection(lines, "diagnostics", diagnostics.title());
            if (inspectorSections.expanded("diagnostics")) {
                for (EditorDiagnostics.Entry entry : diagnostics.entries()) {
                    appendInspectorLine(lines, entry.severity().name() + ": " + entry.message(), entry.severity() == EditorDiagnostics.Severity.ERROR ? 0xFFFF968C : 0xFFFFC778, null, false);
                }
                if (diagnostics.omitted() > 0) {
                    appendInspectorLine(lines, "... " + diagnostics.omitted() + " more issues (see latest.log)", 0xFF96AEBE, null, false);
                }
            }
        }
        if (inspectorTabs.active(EditorInspectorTabs.Page.MORE)) {
            moreActions.put(lines.size(), "reset_all");
            appendInspectorLine(lines, confirmResetAll ? "Confirm: reset ALL Screen changes" : "Reset all Screen changes", 0xFFFFAF9B, null, false);
        }
        if (selected == null) {
            appendInspectorLine(lines, "Select a widget to edit", SELECTED, null, false);
            appendInspectorLine(lines, "Click its outline or the Widgets list", 0xFFE5EEF5, null, false);
            appendInspectorLine(lines, "Drag to move | Alt to ignore snap", 0xFF96AEBE, null, false);
            return lines;
        }

        if (inspectorTabs.active(EditorInspectorTabs.Page.POSITION)) {
        appendSection(lines, "widget", "POSITION & VISIBILITY");
        if (inspectorSections.expanded("widget")) {
            appendInspectorLine(lines, "R: reset individual property", 0xFF96AEBE, null, false);
            appendInspectorLine(lines, "id: " + selected.id(), 0xFFE5EEF5, null, false);
            appendInspectorLine(lines, "container: " + target.editorStructuralParent(selected.id()), 0xFFE5EEF5, null, false);
            AnchorLayout.Bounds bounds = selected.bounds();
            appendInspectorLine(lines, "screen: " + bounds.x() + ", " + bounds.y(), 0xFFE5EEF5, null, false);
            appendInspectorLine(lines, "size: " + bounds.width() + " x " + bounds.height(), 0xFFE5EEF5, null, false);
            appendProperty(lines, "user offset: " + selected.offset().x() + ", " + selected.offset().y(), AnchorHudManager.EditableProperty.OFFSET);
            appendProperty(lines, "visible: " + selected.visible(), AnchorHudManager.EditableProperty.VISIBILITY);
        }
        List<PositionElement.Constraint> anchors = target.editorAnchorConstraints(selected.id());
        if (!anchors.isEmpty()) {
            appendSection(lines, "anchors", "ANCHORS (" + anchors.size() + ")");
            if (inspectorSections.expanded("anchors")) {
                appendAnchorAction(lines, "[+] Add anchor", -1, "add");
                appendAnchorAction(lines, "Reset anchors to resource defaults", -1, "reset");
                for (int index = 0; index < anchors.size(); index++) {
                    PositionElement.Constraint entry = anchors.get(index);
                    String section = "constraint:" + selected.id() + ":" + index;
                    appendSection(lines, section, (focusedAnchorIndex == index ? "  > #" : "  #") + (index + 1) + (index == 0 ? " (primary)" : "") + " " + entry.point() + " -> " + entry.anchor());
                    if (!inspectorSections.expanded(section)) {
                        continue;
                    }
                    appendAnchorAction(lines, "    Point: " + entry.point() + " ▼", index, "point");
                    appendAnchorAction(lines, "    Anchor: " + entry.anchor() + " ▼", index, "anchor");
                    appendAnchorAction(lines, "    Relative to: " + entry.relativeTo() + " ▼", index, "relativeTo");
                    appendAnchorAction(lines, "    X: " + entry.x() + " (edit)", index, "x");
                    appendAnchorAction(lines, "    Y: " + entry.y() + " (edit)", index, "y");
                    if (anchors.size() > 1) {
                        appendAnchorAction(lines, "    [-] Delete this anchor", index, "delete");
                    }
                }
            }
        }
        }
        if (inspectorTabs.active(EditorInspectorTabs.Page.LAYOUT)) {
        List<LayoutInspectorControls.Control> layoutControls = target.editorLayoutControls(selected.id());
        if (!layoutControls.isEmpty()) {
            appendSection(lines, "layout", "COMPONENT / LAYOUT");
            if (inspectorSections.expanded("layout")) {
                appendInspectorLine(lines, "- / + adjust | R reset", 0xFF96AEBE, null, false);
                for (LayoutInspectorControls.Control control : layoutControls) {
                    String label = control.booleanField() ? control.label() : control.label() + ": " + control.value();
                    int width = Math.max(38, (int) ((inspectorWidth - 98) / INSPECTOR_SCALE));
                    String clipped = font.plainSubstrByWidth(label, width);
                    lines.add(new InspectorLine(Component.literal(clipped).getVisualOrderText(), control.modified() ? 0xFF92DFCE : 0xFFE5EEF5, null, control.modified(), control));
                }
            }
        }
        }
        if (inspectorTabs.active(EditorInspectorTabs.Page.STYLE)) {
        List<AppearanceInspectorControls.Control> appearanceControls = target.editorAppearanceControls(selected.id());
        if (!appearanceControls.isEmpty()) {
            appendSection(lines, "visual", "VISUAL / CONTENT");
            if (inspectorSections.expanded("visual")) {
                for (AppearanceInspectorControls.Control control : appearanceControls) {
                    String label = control.booleanField() ? control.field().label() : control.field().label() + ": " + control.value();
                    String clipped = font.plainSubstrByWidth(label, Math.max(38, (int) ((inspectorWidth - 98) / INSPECTOR_SCALE)));
                    lines.add(new InspectorLine(Component.literal(clipped).getVisualOrderText(), control.modified() ? 0xFF92DFCE : 0xFFE5EEF5, null, control.modified(), null, control));
                }
            }
        }
        }
        if (inspectorTabs.active(EditorInspectorTabs.Page.MOTION)) {
        appendSection(lines, "animation", "ANIMATIONS");
        if (inspectorSections.expanded("animation")) {
            appendAnimationAction(lines, "[+] Add animation", "add", -1);
            JsonArray tracks = target.editorAnimations(selected.id());
            List<AppearanceInspectorControls.Control> controls = AnimationInspectorControls.describe(tracks, target.editorDefaultAnimations(selected.id()));
            for (int index = 0; index < tracks.size(); index++) {
                JsonObject track = tracks.get(index).getAsJsonObject();
                String trackId = track.get("id").getAsString();
                String trigger = track.has("trigger") ? track.get("trigger").getAsString() : "open";
                String key = "track:" + selected.id() + ":" + trackId;
                appendSection(lines, key, "  " + trackId + " (" + trigger + ")");
                if (!inspectorSections.expanded(key)) {
                    continue;
                }
                String prefix = "animations.t" + index + ".";
                for (AppearanceInspectorControls.Control control : controls) {
                    if (control.field().path().startsWith(prefix)) {
                        String label = control.booleanField() ? control.field().label() : control.field().label() + ": " + control.value();
                        String clipped = font.plainSubstrByWidth(label, Math.max(38, (int) ((inspectorWidth - 98) / INSPECTOR_SCALE)));
                        lines.add(new InspectorLine(Component.literal(clipped).getVisualOrderText(), control.modified() ? 0xFF92DFCE : 0xFFE5EEF5, null, control.modified(), null, control));
                    }
                }
                appendAnimationAction(lines, "    Duplicate " + trackId, "duplicate", index);
                appendAnimationAction(lines, "    Move up", "up", index);
                appendAnimationAction(lines, "    Move down", "down", index);
                appendAnimationAction(lines, "    Delete " + trackId, "delete", index);
            }
        }
        }
        if (inspectorTabs.active(EditorInspectorTabs.Page.MORE)) {
            appendSection(lines, "source", "SOURCE JSON (read only)");
        if (inspectorSections.expanded("source")) {
            if (!selected.id().equals(inspectedId)) {
                JsonObject settings = target.editorSourceSettings(selected.id());
                String widgetParent = target.editorStructuralParent(selected.id());
                sourceProperties = HudInspectorProperties.flatten(settings, "(none)".equals(widgetParent) ? null : widgetParent);
                inspectedId = selected.id();
            }
            for (String property : sourceProperties) {
                appendInspectorLine(lines, property, 0xFFB7CEDF, null, false);
            }
        }
        }
        return lines;
    }

    private boolean clickMoreAction(double mouseY) {
        int row = (int) Math.floor((mouseY - inspectorTop() + inspectorScroll) / INSPECTOR_LINE_HEIGHT);
        if (!"reset_all".equals(moreActions.get(row))) {
            return false;
        }
        if (!confirmResetAll) {
            confirmResetAll = true;
            status = "Click again to reset every Screen change";
            return true;
        }
        confirmResetAll = false;
        resetAll();
        return true;
    }

    private void appendAnchorAction(List<InspectorLine> lines, String text, int index, String field) {
        anchorActions.put(lines.size(), new AnchorAction(index, field));
        String label = font.plainSubstrByWidth(text, Math.max(24, (int) ((inspectorWidth - 28) / INSPECTOR_SCALE)));
        lines.add(new InspectorLine(Component.literal(label).getVisualOrderText(), "delete".equals(field) ? 0xFFFF9887 : 0xFF92DFCE, null, false));
    }

    private void appendAnimationAction(List<InspectorLine> lines, String title, String action, int index) {
        animationActions.put(lines.size(), new AnimationAction(action, index));
        appendInspectorLine(lines, title, action.equals("delete") ? 0xFFFF9887 : 0xFF92DFCE, null, false);
    }

    private boolean clickAnimationAction(double mouseY) {
        int row = (int) Math.floor((mouseY - inspectorTop() + inspectorScroll) / INSPECTOR_LINE_HEIGHT);
        AnimationAction action = animationActions.get(row);
        if (action == null || selected == null) {
            return false;
        }
        try {
            JsonArray before = target.editorAnimations(selected.id());
            JsonArray after = switch (action.operation()) {
                case "add" -> AnimationInspectorControls.create(before);
                case "duplicate" -> AnimationInspectorControls.duplicate(before, action.trackIndex());
                case "delete" -> AnimationInspectorControls.delete(before, action.trackIndex());
                case "up" -> AnimationInspectorControls.move(before, action.trackIndex(), -1);
                case "down" -> AnimationInspectorControls.move(before, action.trackIndex(), 1);
                default -> before;
            };
            status = target.editorSetAnimations(selected.id(), after) ? "" : target.screenSaveError();
            refreshSelection();
        } catch (RuntimeException exception) {
            status = exception.getMessage();
        }
        return true;
    }

    private void appendSection(List<InspectorLine> lines, String id, String title) {
        inspectorSections.register(id, lines.size());
        String label = inspectorSections.label(id, title);
        String clipped = font.plainSubstrByWidth(label, Math.max(24, (int) ((inspectorWidth - 22) / INSPECTOR_SCALE)));
        lines.add(new InspectorLine(Component.literal(clipped).getVisualOrderText(), SELECTED, null, false));
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
            inspectorScrollBar = new NativeContainerScrollPanel(minecraft, viewport, maxInspectorScroll, 23, 1.0, inspectorScroll, NativeContainerScrollPanel.SCREEN_INSPECTOR);
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
        if (textureBrowser != null) {
            return textureBrowser.mouseClicked(mouseX, mouseY, button);
        }
        if (exitConfirmation.isOpen()) {
            return exitConfirmation.mouseClicked(mouseX, mouseY, button);
        }
        if (colorPickerDialog != null) {
            return colorPickerDialog.mouseClicked(mouseX, mouseY, button);
        }
        if (editorDropdown.isOpen()) {
            return editorDropdown.mouseClicked(mouseX, mouseY, button);
        }
        if (editingAppearance != null || editingAnchor != null) {
            return clickAppearanceModal(mouseX, mouseY, button);
        }
        if (!compact && button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && mouseX >= visiblePanelX() + visiblePanelWidth() - 30
                && mouseX < visiblePanelX() + visiblePanelWidth()
                && mouseY >= panelTop && mouseY < panelTop + 27) {
            toggleInspector();
            return true;
        }
        if (!compact && !inspectorCollapsed && button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && inspectorTabs.click(mouseX, mouseY, panelX() + 7, panelTop + 77, inspectorWidth - 14)) {
            resetInspectorScroll();
            confirmResetAll = false;
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
                    && widgetList.overlaps(panelX(), panelTop, panelX() + inspectorWidth, panelTop + expandedPanelHeight())) {
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
                String section = inspectorSections.headerAt(mouseY, inspectorTop(), inspectorBottom(), inspectorScroll, INSPECTOR_LINE_HEIGHT);
                if (section != null) {
                    String prefix = selected == null ? "" : "constraint:" + selected.id() + ":";
                    if (!prefix.isEmpty() && section.startsWith(prefix)) {
                        focusedAnchorIndex = Integer.parseInt(section.substring(prefix.length()));
                    }
                    inspectorSections.toggle(section);
                    return true;
                }
                if (clickMoreAction(mouseY) || clickAnchorAction(mouseX, mouseY) || clickAnimationAction(mouseY)) {
                    return true;
                }
                for (AppearanceHitbox hitbox : appearanceHitboxes) {
                    if (hitbox.contains(mouseX, mouseY)) {
                        if (hitbox.direction() == 0) {
                            status = target.editorResetAppearance(selected.id(), hitbox.control())
                                    ? "" : target.screenSaveError();
                            refreshSelection();
                        } else if (hitbox.direction() == 2) {
                            openAppearanceInput(selected.id(), hitbox.control());
                        } else if (hitbox.direction() == 3) {
                            openAppearanceDropdown(hitbox);
                        } else {
                            try {
                                String value = AppearanceInspectorControls.stepped(hitbox.control(), hitbox.direction()).getAsString();
                                status = target.editorSetAppearance(selected.id(), hitbox.control(), value)
                                        ? "" : target.screenSaveError();
                                refreshSelection();
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
                        boolean changed = selected != null && (hitbox.direction() == 0
                                ? target.editorResetLayout(selected.id(), hitbox.control())
                                : target.editorAdjustLayout(selected.id(), hitbox.control(), hitbox.direction()));
                        status = changed ? "" : target.editorLayoutFailure();
                        refreshSelection();
                        return true;
                    }
                }
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
            ItemLoreScreen.EditorHandle panel = hitParentPanel(mouseX, mouseY);
            chooseParentTarget(panel == null ? "screen" : panel.id());
            return true;
        }
        select(hit == null ? null : hit.id());
        if (selected != null) {
            AnchorLayout.Bounds bounds = selected.bounds();
            dragOrigin = new HudSnap.Widget(selected.id(), selected.relativeTo(), bounds.x(), bounds.y(), bounds.width(), bounds.height(), selected.visible());
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
        if (textureBrowser != null) {
            return textureBrowser.mouseDragged(mouseX, mouseY, button);
        }
        if (exitConfirmation.isOpen()) {
            return true;
        }
        if (colorPickerDialog != null) {
            return colorPickerDialog.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
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
            panelTop = Math.max(0, Math.min(height - expandedPanelHeight(), panelDragTop + (int) Math.round(mouseY - panelStartY)));
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
                targets.add(new HudSnap.Widget(handle.id(), handle.relativeTo(), b.x(), b.y(), b.width(), b.height(), handle.visible()));
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
        if (textureBrowser != null) {
            return textureBrowser.mouseReleased(mouseX, mouseY, button);
        }
        if (exitConfirmation.isOpen()) {
            return true;
        }
        if (colorPickerDialog != null) {
            return colorPickerDialog.mouseReleased(mouseX, mouseY, button);
        }
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
        if (textureBrowser != null) {
            return textureBrowser.mouseScrolled(mouseX, mouseY, scrollY);
        }
        if (exitConfirmation.isOpen()) {
            return true;
        }
        if (colorPickerDialog != null) {
            return true;
        }
        if (editorDropdown.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
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
        if (textureBrowser != null) {
            return textureBrowser.keyPressed(keyCode);
        }
        if (exitConfirmation.isOpen()) {
            return exitConfirmation.keyPressed(keyCode);
        }
        if (colorPickerDialog != null) {
            return colorPickerDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (editorDropdown.keyPressed(keyCode)) {
            return true;
        }
        if (editingAppearance != null || editingAnchor != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeAppearanceInput();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commitAppearance();
                return true;
            }
            return appearanceInput.keyPressed(keyCode, scanCode, modifiers);
        }
        if (KeyRegistry.EDIT_HUD.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            requestClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (pickingParent) {
                pickingParent = false;
                status = "";
                updateControls();
                return true;
            }
            requestClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            editorDropdown.close();
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
    public boolean charTyped(char codePoint, int modifiers) {
        if (textureBrowser != null) {
            return true;
        }
        if (exitConfirmation.isOpen()) {
            return true;
        }
        if (colorPickerDialog != null) {
            return colorPickerDialog.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        requestClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
