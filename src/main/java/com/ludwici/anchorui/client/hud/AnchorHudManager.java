package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.condition.BoundCondition;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.layout.ContainerLayout;
import com.ludwici.anchorui.api.layout.StackLayout;
import com.ludwici.anchorui.api.layout.ContainerAutoLayout;
import com.ludwici.anchorui.api.layout.ContainerScroll;
import com.ludwici.anchorui.api.layout.WidgetContainers;
import com.ludwici.anchorui.api.layout.WidgetTransforms;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.widget.AnchorWidgetRegistry;
import com.ludwici.anchorui.api.widget.WidgetConfigException;
import com.ludwici.anchorui.api.widget.WidgetContext;
import com.ludwici.anchorui.api.widget.WidgetIds;
import com.ludwici.anchorui.api.widget.WidgetInstanceRegistry;
import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.gui.AnchoredWidget;
import com.ludwici.anchorui.client.gui.NativeContainerScrollPanel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AnchorHudManager {
    public static final AnchorHudManager INSTANCE = new AnchorHudManager();

    public enum EditableProperty {
        OFFSET, PARENT, POINT, ANCHOR, VISIBILITY
    }

    public record HudHandle(String id, AnchorLayout.Bounds bounds, String relativeTo, ElementAnchor point, ElementAnchor anchor, HudLayoutOffsets.Offset offset, boolean visible, boolean hiddenByUser,
                            String defaultParent, ElementAnchor defaultPoint, ElementAnchor defaultAnchor, boolean defaultVisible) {
        public boolean isModified(EditableProperty property) {
            return switch (property) {
                case OFFSET -> !offset.equals(HudLayoutOffsets.Offset.ZERO);
                case PARENT -> !relativeTo.equals(defaultParent);
                case POINT -> point != defaultPoint;
                case ANCHOR -> anchor != defaultAnchor;
                case VISIBILITY -> hiddenByUser;
            };
        }
    }

    private record Description(String id, LoreEntity entity, LoreEntity.UiElement element) {
    }

    private record PlacedWidget(String id, AbstractWidget widget, LoreStyle.Element style, PositionElement position,
                                boolean baseVisible, boolean baseActive, WidgetContext context,
                                BoundCondition visibleWhen, BoundCondition enabledWhen, String containerParent,
                                boolean clipChildren, WidgetTransforms.Group group, ContainerLayout.AutoSize autoSize,
                                ContainerScroll.Config scroll, int preferredWidth, int preferredHeight) {
        private PositionElement effectivePosition(HudLayoutOffsets offsets) {
            HudLayoutOffsets.Offset delta = offsets.get(id);
            HudLayoutOffsets.AnchorOverride anchor = offsets.anchor(id);
            ElementAnchor point = anchor == null ? position.point : anchor.point();
            ElementAnchor targetPoint = anchor == null ? position.anchor : anchor.anchor();
            String relativeTo = anchor == null ? position.relativeTo : anchor.relativeTo();
            return new PositionElement(point, targetPoint, relativeTo, position.x + delta.x(), position.y + delta.y());
        }

        private AnchorLayout.Frame frame(HudLayoutOffsets offsets) {
            return new AnchorLayout.Frame(id, effectivePosition(offsets), preferredWidth, preferredHeight);
        }
    }

    private WidgetInstanceRegistry registry = new WidgetInstanceRegistry();
    private List<PlacedWidget> widgets = List.of();
    private Map<String, AnchorLayout.Bounds> layoutBounds = Map.of();
    private Set<String> collapsingLayoutParents = Set.of();
    private Set<String> collapsedLayoutHidden = Set.of();
    private Map<String, VanillaHudGeometry.Reference> vanillaReferences = Map.of();
    private List<VanillaHudGeometry.Reference> vanillaHandles = List.of();
    private VanillaHudAnchors.Snapshot vanillaSnapshot;
    private final Map<String, Boolean> visibilityScratch = new HashMap<>();
    private final Map<String, Boolean> enabledScratch = new HashMap<>();
    private Map<String, AnchorLayout.Bounds> clipBounds = Map.of();
    private Map<String, WidgetTransforms.Transform> transforms = Map.of();
    private Map<String, ContainerScroll.Limits> scrollLimits = Map.of();
    private final Map<String, ContainerScroll.Offset> scrollOffsets = new HashMap<>();
    private final Map<String, NativeContainerScrollPanel> nativeScrollPanels = new LinkedHashMap<>();
    private NativeContainerScrollPanel draggedScrollPanel;
    private Map<String, String> containerParents = Map.of();
    private Map<String, WidgetTransforms.Group> containerGroups = Map.of();
    private Set<String> clippingContainers = Set.of();
    private final Set<String> renderFailures = new HashSet<>();
    private HudLayoutOffsets savedOffsets = new HudLayoutOffsets();
    private HudLayoutOffsets editingOffsets;
    private boolean storageLoaded;
    private String lastSaveError;
    private long builtRevision = -1;
    private int builtWidth = -1;
    private int builtHeight = -1;
    private ClientLevel builtLevel;
    private Set<String> builtTestGroups = Set.of();
    private final Set<String> enabledTestGroups = new HashSet<>();

    private AnchorHudManager() {
    }

    public boolean isTestEnabled() {
        return isTestEnabled("hud1");
    }

    public boolean isTestEnabled(String group) {
        return enabledTestGroups.contains(group);
    }

    public void setTestEnabled(boolean enabled) {
        setTestEnabled("hud1", enabled);
    }

    public void setTestEnabled(String group, boolean enabled) {
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("HUD test group must not be blank");
        }
        boolean changed = enabled ? enabledTestGroups.add(group) : enabledTestGroups.remove(group);
        if (changed) {
            builtRevision = -1;
        }
    }

    public boolean isEditing() {
        return editingOffsets != null;
    }

    public void beginEditing() {
        ensureStorageLoaded();
        if (editingOffsets == null) {
            editingOffsets = savedOffsets.copy();
            relayout(null);
        }
    }

    public HudLayoutOffsets.Offset getOffset(String id) {
        return currentOffsets().get(id);
    }

    public void setPreviewOffset(String id, int x, int y) {
        if (editingOffsets == null || !isCustomWidget(id)) {
            return;
        }
        editingOffsets.set(id, x, y);
        relayout(null);
    }

    public void togglePreviewHidden(String id) {
        if (editingOffsets == null || !isCustomWidget(id)) {
            return;
        }
        editingOffsets.setHidden(id, !editingOffsets.isHidden(id));
        applyVisibility();
    }

    public boolean cyclePreviewPoint(String id) {
        HudHandle handle = handle(id);
        if (handle == null) {
            return false;
        }
        ElementAnchor[] values = ElementAnchor.values();
        ElementAnchor next = values[(handle.point().ordinal() + 1) % values.length];
        return changePreviewAnchor(id, new HudLayoutOffsets.AnchorOverride(handle.relativeTo(), next, handle.anchor()));
    }

    public boolean cyclePreviewAnchor(String id) {
        HudHandle handle = handle(id);
        if (handle == null) {
            return false;
        }
        ElementAnchor[] values = ElementAnchor.values();
        ElementAnchor next = values[(handle.anchor().ordinal() + 1) % values.length];
        return changePreviewAnchor(id, new HudLayoutOffsets.AnchorOverride(handle.relativeTo(), handle.point(), next));
    }

    public boolean setPreviewParent(String id, String parent) {
        HudHandle handle = handle(id);
        if (handle == null || id.equals(parent)) {
            return false;
        }
        if (!"screen".equals(parent) && !layoutBounds.containsKey(parent)) {
            return false;
        }
        return changePreviewAnchor(id, new HudLayoutOffsets.AnchorOverride(parent, handle.point(), handle.anchor()));
    }

    private HudHandle handle(String id) {
        if (editingOffsets == null) {
            return null;
        }
        for (HudHandle handle : getHandles()) {
            if (handle.id().equals(id)) {
                return handle;
            }
        }
        return null;
    }

    private boolean changePreviewAnchor(String id, HudLayoutOffsets.AnchorOverride override) {
        if (editingOffsets == null) {
            return false;
        }
        if (!isCustomWidget(id)) {
            return false;
        }
        HudLayoutOffsets candidate = editingOffsets.copy();
        for (PlacedWidget placed : widgets) {
            if (placed.id().equals(id) && placed.position().relativeTo.equals(override.relativeTo())
                    && placed.position().point == override.point() && placed.position().anchor == override.anchor()) {
                candidate.clearAnchor(id);
                break;
            }
            if (placed.id().equals(id)) {
                candidate.setAnchor(id, override);
                break;
            }
        }
        try {
            resolveBounds(candidate);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        editingOffsets = candidate;
        relayout(null);
        return true;
    }

    private boolean isCustomWidget(String id) {
        return registry.definition(id).isPresent();
    }

    public List<VanillaHudGeometry.Reference> getVanillaHandles() {
        return vanillaHandles;
    }

    private ContainerLayout.Result resolveLayout(HudLayoutOffsets offsets, List<String> warnings) {
        List<AnchorLayout.Frame> frames = new ArrayList<>();
        if (!vanillaReferences.isEmpty()) {
            frames.addAll(VanillaHudGeometry.frames(vanillaReferences));
        }
        Map<String, PaddingElement> containers = new LinkedHashMap<>();
        Map<String, ContainerLayout.AutoSize> autoSizes = new LinkedHashMap<>();
        Map<String, ContainerAutoLayout> stackLayouts = new LinkedHashMap<>();
        Map<String, StackLayout.Rules> itemRules = new LinkedHashMap<>();
        Map<String, String> parents = new LinkedHashMap<>();
        Set<String> hidden = new HashSet<>();
        Set<String> manual = new HashSet<>();
        for (PlacedWidget placed : widgets) {
            frames.add(placed.frame(offsets));
            if (placed.widget() instanceof com.ludwici.anchorui.client.gui.PanelWidget) {
                containers.put(placed.id(), placed.style().padding);
                var layoutSettings = placed.style().resolvedSettings;
                ContainerAutoLayout stack = layoutSettings.has("type")
                        && WidgetContainers.supportsChildren(layoutSettings.get("type").getAsString())
                        ? ContainerAutoLayout.parse(layoutSettings) : null;
                if (stack != null) {
                    stackLayouts.put(placed.id(), stack);
                }
                if (placed.autoSize().enabled()) {
                    autoSizes.put(placed.id(), placed.autoSize());
                }
            }
            if (placed.containerParent() != null) {
                parents.put(placed.id(), placed.containerParent());
            }
            StackLayout.Rules rules = StackLayout.Rules.parse(placed.style().resolvedSettings);
            itemRules.put(placed.id(), rules);
            if (!rules.managed()) {
                manual.add(placed.id());
            }
            if (!placed.widget().visible) {
                hidden.add(placed.id());
            }
        }
        Set<String> collapsing = new HashSet<>();
        for (var entry : stackLayouts.entrySet()) {
            if (entry.getValue().collapseHidden()) {
                collapsing.add(entry.getKey());
            }
        }
        collapsingLayoutParents = Set.copyOf(collapsing);
        collapsedLayoutHidden = relevantLayoutHidden(hidden, parents, collapsingLayoutParents);
        return ContainerLayout.resolve(builtWidth, builtHeight, frames, containers, autoSizes, parents,
                stackLayouts, itemRules, hidden, manual, warning -> {
                    if (warnings != null) {
                        warnings.add(warning);
                    }
                });
    }

    private static Set<String> relevantLayoutHidden(Set<String> hidden, Map<String, String> parents,
                                                     Set<String> collapsingParents) {
        Set<String> result = new HashSet<>();
        for (var entry : parents.entrySet()) {
            if (collapsingParents.contains(entry.getValue()) && hidden.contains(entry.getKey())) {
                result.add(entry.getKey());
            }
        }
        return Set.copyOf(result);
    }

    private Set<String> currentLayoutHidden() {
        Set<String> hidden = new HashSet<>();
        Map<String, String> parents = new LinkedHashMap<>();
        for (PlacedWidget placed : widgets) {
            if (placed.containerParent() != null) {
                parents.put(placed.id(), placed.containerParent());
            }
            if (!placed.widget().visible) {
                hidden.add(placed.id());
            }
        }
        return relevantLayoutHidden(hidden, parents, collapsingLayoutParents);
    }

    private Map<String, AnchorLayout.Bounds> resolveBounds(HudLayoutOffsets offsets) {
        return resolveLayout(offsets, null).bounds();
    }

    private void updateScrolledTransforms() {
        transforms = WidgetTransforms.resolve(layoutBounds, containerParents, containerGroups, scrollOffsets);
        clipBounds = WidgetContainers.clips(layoutBounds, containerParents, clippingContainers, transforms);
        refreshNativeScrollPanels();
    }

    private void refreshNativeScrollPanels() {
        Map<String, NativeContainerScrollPanel> panels = new LinkedHashMap<>();
        for (PlacedWidget placed : widgets) {
            if (!placed.scroll().vertical()) {
                continue;
            }
            ContainerScroll.Limits limits = scrollLimits.get(placed.id());
            AnchorLayout.Bounds content = layoutBounds.get(WidgetContainers.contentId(placed.id()));
            if (limits == null || limits.y() == 0 || content == null) {
                continue;
            }
            WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
            AnchorLayout.Bounds viewport = transform.physical(content);
            if (viewport.width() <= 6 || viewport.height() < 34) {
                continue;
            }
            NativeContainerScrollPanel panel = nativeScrollPanels.get(placed.id());
            if (panel == null || !panel.matches(viewport, limits.y(), placed.scroll().step(), transform.scale())) {
                ContainerScroll.Offset offset = scrollOffsets.getOrDefault(placed.id(), ContainerScroll.Offset.ZERO);
                panel = new NativeContainerScrollPanel(Minecraft.getInstance(), viewport, limits.y(),
                        placed.scroll().step(), transform.scale(), offset.y());
            }
            panels.put(placed.id(), panel);
        }
        nativeScrollPanels.clear();
        nativeScrollPanels.putAll(panels);
    }

    private void storeNativeOffset(String id, NativeContainerScrollPanel panel) {
        ContainerScroll.Offset previous = scrollOffsets.getOrDefault(id, ContainerScroll.Offset.ZERO);
        int position = panel.logicalOffset();
        if (position != previous.y()) {
            scrollOffsets.put(id, new ContainerScroll.Offset(previous.x(), position));
            updateScrolledTransforms();
        }
    }

    public boolean mouseClickedScrollBar(double mouseX, double mouseY, int button) {
        if (editingOffsets == null) {
            return false;
        }
        for (int index = widgets.size() - 1; index >= 0; index--) {
            PlacedWidget placed = widgets.get(index);
            NativeContainerScrollPanel panel = nativeScrollPanels.get(placed.id());
            AnchorLayout.Bounds clip = clipBounds.get(placed.id());
            if (panel != null && placed.widget().visible && placed.widget().active
                    && (clip == null || WidgetContainers.contains(clip, mouseX, mouseY))
                    && panel.mouseClicked(mouseX, mouseY, button)) {
                draggedScrollPanel = panel;
                return true;
            }
        }
        return false;
    }

    public boolean mouseDraggedScrollBar(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggedScrollPanel == null) {
            return false;
        }
        NativeContainerScrollPanel panel = draggedScrollPanel;
        if (!panel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return false;
        }
        for (var entry : nativeScrollPanels.entrySet()) {
            if (entry.getValue() == panel) {
                storeNativeOffset(entry.getKey(), panel);
                break;
            }
        }
        return true;
    }

    public boolean mouseReleasedScrollBar(double mouseX, double mouseY, int button) {
        if (draggedScrollPanel == null) {
            return false;
        }
        NativeContainerScrollPanel panel = draggedScrollPanel;
        draggedScrollPanel = null;
        return panel.mouseReleased(mouseX, mouseY, button);
    }

    public void renderScrollBars(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (editingOffsets == null) {
            return;
        }
        for (PlacedWidget placed : widgets) {
            if (!placed.widget().visible) {
                continue;
            }
            NativeContainerScrollPanel panel = nativeScrollPanels.get(placed.id());
            if (panel != null) {
                panel.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    public boolean scrollContentsAt(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (editingOffsets == null) {
            return false;
        }
        for (int index = widgets.size() - 1; index >= 0; index--) {
            PlacedWidget placed = widgets.get(index);
            if (!placed.widget().visible || !placed.widget().active || !placed.scroll().enabled()) {
                continue;
            }
            AnchorLayout.Bounds clip = clipBounds.get(placed.id());
            if (clip != null && !WidgetContainers.contains(clip, mouseX, mouseY)) {
                continue;
            }
            AnchorLayout.Bounds content = layoutBounds.get(WidgetContainers.contentId(placed.id()));
            if (content == null) {
                continue;
            }
            WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
            if (!WidgetContainers.contains(transform.physical(content), mouseX, mouseY)) {
                continue;
            }
            ContainerScroll.Limits limits = scrollLimits.get(placed.id());
            if (limits == null) {
                continue;
            }

            ContainerScroll.Offset old = scrollOffsets.getOrDefault(placed.id(), ContainerScroll.Offset.ZERO);
            NativeContainerScrollPanel panel = nativeScrollPanels.get(placed.id());
            int nextY = old.y();
            if (panel != null && scrollY != 0) {
                panel.mouseScrolled(mouseX, mouseY, 0, scrollY);
                nextY = panel.logicalOffset();
            }
            int nextX = old.x();
            if (limits.x() > 0 && placed.scroll().horizontal()) {
                double horizontalWheel = scrollX != 0 ? scrollX : !placed.scroll().vertical() ? scrollY : 0;
                nextX = Math.max(0, Math.min(limits.x(),
                        old.x() - (int) Math.round(horizontalWheel * placed.scroll().step())));
            }
            if (panel == null && placed.scroll().vertical()) {
                nextY = ContainerScroll.move(old, limits, placed.scroll(), 0, scrollY).y();
            }
            ContainerScroll.Offset next = new ContainerScroll.Offset(nextX, nextY);
            if (!next.equals(old)) {
                scrollOffsets.put(placed.id(), next);
                updateScrolledTransforms();
                return true;
            }
        }
        return false;
    }

    private void applyVisibility() {
        HudLayoutOffsets offsets = currentOffsets();
        visibilityScratch.clear();
        enabledScratch.clear();
        for (PlacedWidget placed : widgets) {
            String parent = placed.containerParent();
            boolean parentVisible = parent == null || visibilityScratch.getOrDefault(parent, false);
            boolean parentEnabled = parent == null || enabledScratch.getOrDefault(parent, false);
            boolean visible = parentVisible && placed.baseVisible() && !offsets.isHidden(placed.id())
                    && placed.visibleWhen().get(placed.context());
            boolean allowed = visible && parentEnabled && placed.enabledWhen().get(placed.context());
            placed.widget().visible = visible;
            placed.widget().active = allowed && placed.baseActive();
            visibilityScratch.put(placed.id(), visible);
            enabledScratch.put(placed.id(), allowed);
        }
    }

    public boolean resetPreviewProperty(String id, EditableProperty property) {
        if (editingOffsets == null || !isCustomWidget(id)) {
            return false;
        }
        PlacedWidget placed = null;
        for (PlacedWidget candidate : widgets) {
            if (candidate.id().equals(id)) {
                placed = candidate;
                break;
            }
        }
        if (placed == null) {
            return false;
        }
        switch (property) {
            case OFFSET -> {
                editingOffsets.set(id, 0, 0);
                relayout(null);
                return true;
            }
            case VISIBILITY -> {
                editingOffsets.setHidden(id, false);
                applyVisibility();
                return true;
            }
            case PARENT, POINT, ANCHOR -> {
                HudHandle current = handle(id);
                if (current == null) {
                    return false;
                }
                PositionElement defaults = placed.position();
                String parent = property == EditableProperty.PARENT ? defaults.relativeTo : current.relativeTo();
                ElementAnchor point = property == EditableProperty.POINT ? defaults.point : current.point();
                ElementAnchor anchor = property == EditableProperty.ANCHOR ? defaults.anchor : current.anchor();
                return changePreviewAnchor(id, new HudLayoutOffsets.AnchorOverride(parent, point, anchor));
            }
        }
        return false;
    }

    public void resetPreviewOffset(String id) {
        if (editingOffsets != null && isCustomWidget(id)) {
            editingOffsets.reset(id);
            relayout(null);
        }
    }

    public void resetAllPreviewOffsets() {
        if (editingOffsets != null) {
            editingOffsets.resetAll();
            relayout(null);
        }
    }

    public boolean hasUnsavedChanges() {
        return editingOffsets != null && !editingOffsets.sameAs(savedOffsets);
    }

    public boolean saveEditing() {
        if (editingOffsets == null) {
            return true;
        }
        try {
            HudLayoutStorage.save(configPath(), editingOffsets);
            savedOffsets = editingOffsets.copy();
            editingOffsets = null;
            lastSaveError = null;
            return true;
        } catch (IOException | RuntimeException exception) {
            lastSaveError = exception.getMessage();
            AnchorUI.LOGGER.error("Failed to save Anchor UI HUD positions", exception);
            return false;
        }
    }

    public String getLastSaveError() {
        return lastSaveError;
    }

    public void cancelEditing() {
        if (editingOffsets != null) {
            editingOffsets = null;
            relayout(null);
        }
    }

    public JsonObject getWidgetSettings(String id) {
        return registry.definition(id).map(element -> element.settings.deepCopy()).orElse(null);
    }

    public double dragScale(String id) {
        WidgetTransforms.Transform transform = transforms.getOrDefault(id, WidgetTransforms.IDENTITY);
        for (PlacedWidget placed : widgets) {
            if (placed.id().equals(id)) {
                return transform.scale() / (placed.group() == null ? 1.0 : placed.group().scale());
            }
        }
        return 1.0;
    }

    public List<HudHandle> getHandles() {
        List<HudHandle> result = new ArrayList<>();
        HudLayoutOffsets offsets = currentOffsets();
        for (PlacedWidget placed : widgets) {
            AnchorLayout.Bounds bounds = layoutBounds.get(placed.id());
            if (bounds != null && !renderFailures.contains(placed.id())) {
                PositionElement effective = placed.effectivePosition(offsets);
                WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
                result.add(new HudHandle(placed.id(), transform.physical(bounds), effective.relativeTo, effective.point, effective.anchor, offsets.get(placed.id()), placed.widget().visible, offsets.isHidden(placed.id()),
                        placed.position().relativeTo, placed.position().point, placed.position().anchor, placed.baseVisible()));
            }
        }
        return List.copyOf(result);
    }

    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            clear();
            return;
        }
        if (!HudVisibilityPolicy.shouldRender(true, minecraft.options.hideGui, minecraft.screen != null, minecraft.screen instanceof ChatScreen)) {
            return;
        }
        prepare(minecraft, graphics.guiWidth(), graphics.guiHeight());
        renderWidgets(graphics, delta.getGameTimeDeltaPartialTick(false));
    }

    public void renderEditorPreview(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || editingOffsets == null) {
            return;
        }
        prepare(minecraft, graphics.guiWidth(), graphics.guiHeight());
        renderWidgets(graphics, partialTick);
    }

    private void prepare(Minecraft minecraft, int width, int height) {
        ensureStorageLoaded();
        VanillaHudAnchors.Snapshot snapshot = VanillaHudAnchors.snapshot(minecraft);
        boolean geometryChanged = vanillaSnapshot == null
                || snapshot.heartRows() != vanillaSnapshot.heartRows()
                || snapshot.effectRows() != vanillaSnapshot.effectRows();
        if (vanillaSnapshot == null || !snapshot.equals(vanillaSnapshot)
                || builtWidth != width || builtHeight != height) {
            vanillaReferences = VanillaHudAnchors.measure(snapshot, width, height);
            List<VanillaHudGeometry.Reference> references = new ArrayList<>(VanillaHudGeometry.IDS.size());
            for (String id : VanillaHudGeometry.IDS) {
                references.add(vanillaReferences.get(id));
            }
            vanillaHandles = List.copyOf(references);
            vanillaSnapshot = snapshot;
        }
        long revision = LoreLoader.INSTANCE.getRevision();
        if (builtRevision != revision || builtWidth != width || builtHeight != height || builtLevel != minecraft.level || !builtTestGroups.equals(enabledTestGroups)) {
            rebuild(minecraft, width, height, revision);
        } else if (geometryChanged) {
            relayout(null);
        }
    }

    private HudLayoutOffsets currentOffsets() {
        ensureStorageLoaded();
        return editingOffsets == null ? savedOffsets : editingOffsets;
    }

    private void ensureStorageLoaded() {
        if (storageLoaded) {
            return;
        }
        storageLoaded = true;
        try {
            savedOffsets = HudLayoutStorage.load(configPath());
        } catch (IOException | RuntimeException exception) {
            AnchorUI.LOGGER.warn("Could not read Anchor UI HUD layout; using default positions", exception);
            savedOffsets = new HudLayoutOffsets();
        }
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("anchorui").resolve("hud-layout.json");
    }

    private void renderWidgets(GuiGraphics graphics, float partialTick) {
        applyVisibility();
        if (!collapsingLayoutParents.isEmpty() && !currentLayoutHidden().equals(collapsedLayoutHidden)) {
            relayout(null);
        }
        for (PlacedWidget placed : widgets) {
            if (renderFailures.contains(placed.id())) {
                continue;
            }
            AbstractWidget widget = placed.widget();
            if (!widget.visible) {
                continue;
            }
            AnchorLayout.Bounds clip = clipBounds.get(placed.id());
            if (clip != null && (clip.width() == 0 || clip.height() == 0)) {
                continue;
            }
            graphics.pose().pushPose();
            if (clip != null) {
                graphics.enableScissor(clip.x(), clip.y(), clip.x() + clip.width(), clip.y() + clip.height());
            }
            try {
                int color = placed.style().background.color;
                if (!(widget instanceof AnchoredWidget) && (color >>> 24) != 0) {
                    graphics.fill(widget.getX(), widget.getY(), widget.getRight(), widget.getBottom(), color);
                }
                WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
                graphics.pose().translate(transform.translateX(), transform.translateY(), 0);
                graphics.pose().scale((float) transform.scale(), (float) transform.scale(), 1.0F);
                widget.render(graphics, -1, -1, partialTick);
            } catch (RuntimeException exception) {
                renderFailures.add(placed.id());
                AnchorUI.LOGGER.error("Anchor UI HUD widget '{}' failed during rendering; disabled until rebuild", placed.id(), exception);
            } finally {
                if (clip != null) {
                    graphics.disableScissor();
                }
                graphics.pose().popPose();
            }
        }
    }

    private void rebuild(Minecraft minecraft, int width, int height, long revision) {
        builtRevision = revision;
        builtWidth = width;
        builtHeight = height;
        builtLevel = minecraft.level;
        builtTestGroups = Set.copyOf(enabledTestGroups);
        registry = new WidgetInstanceRegistry();
        widgets = List.of();
        layoutBounds = Map.of();
        collapsingLayoutParents = Set.of();
        collapsedLayoutHidden = Set.of();
        clipBounds = Map.of();
        transforms = Map.of();
        scrollLimits = Map.of();
        scrollOffsets.clear();
        nativeScrollPanels.clear();
        draggedScrollPanel = null;
        renderFailures.clear();

        List<Description> declared = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        var documents = LoreLoader.INSTANCE.getHudDocuments().entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString())).toList();
        for (var document : documents) {
            ResourceLocation source = document.getKey();
            LoreLoader.HudDocument hud = document.getValue();
            if (hud.testOnly() && !enabledTestGroups.contains(hud.testGroup())) {
                continue;
            }
            int index = 0;
            for (LoreEntity.UiElement element : hud.entity().entities) {
                String defaultId = element.type.getPath().replace('/', '_') + "_" + index;
                try {
                    String id = WidgetIds.id(element.id.isBlank() ? defaultId : element.id, hud.entity().namespace);
                    if (VanillaHudGeometry.reserved(id)) {
                        warnings.add("HUD '" + source + "': reserved vanilla HUD reference id '" + id + "' cannot be declared as a widget");
                        index++;
                        continue;
                    }
                    registry.declare(id, element);
                    declared.add(new Description(id, hud.entity(), element));
                } catch (IllegalArgumentException exception) {
                    warnings.add("HUD '" + source + "': " + exception.getMessage());
                }
                index++;
            }
        }

        List<PlacedWidget> created = new ArrayList<>();
        for (Description definition : declared) {
            LoreEntity entity = definition.entity();
            LoreEntity.UiElement element = definition.element();
            var factory = AnchorWidgetRegistry.find(element.type).orElse(null);
            if (factory == null) {
                warnings.add("Unknown HUD widget type '" + element.type + "' on element '" + definition.id() + "'");
                continue;
            }
            try {
                LoreStyle theme = LoreLoader.INSTANCE.getStyle(element.settings.has("style") ? element.settings.get("style").getAsString() : entity.style);
                LoreStyle.Element style = theme.get(element.type, element.settings);
                WidgetContext context = new WidgetContext(minecraft, null, ItemStack.EMPTY, entity, registry);
                AbstractWidget widget = factory.create(context, element, style);
                if (widget == null) {
                    warnings.add("HUD factory returned null for widget '" + definition.id() + "'");
                    continue;
                }
                if (style.size.width > 0) {
                    widget.setWidth(style.size.width);
                }
                if (style.size.height > 0) {
                    widget.setHeight(style.size.height);
                }
                widget.visible = widget.visible && !style.hidden;
                PositionElement position = style.position;
                String relativeTo = WidgetIds.reference(position.relativeTo, entity.namespace);
                PositionElement anchored = new PositionElement(position.point, position.anchor, relativeTo, position.x, position.y);
                BoundCondition visibleWhen = BoundCondition.parse(element.settings, "visibleWhen", true);
                BoundCondition enabledWhen = BoundCondition.parse(element.settings, "enabledWhen", true, false);
                registry.attach(definition.id(), widget);
                boolean panel = widget instanceof com.ludwici.anchorui.client.gui.PanelWidget;
                created.add(new PlacedWidget(definition.id(), widget, style, anchored,
                        widget.visible, widget.active, context, visibleWhen, enabledWhen,
                        WidgetContainers.parent(element.settings),
                        panel && (WidgetContainers.clipChildren(element.settings) || style.scroll.enabled()),
                        panel ? WidgetContainers.group(element.settings) : null,
                        panel ? style.autoSize : ContainerLayout.AutoSize.NONE,
                        panel ? style.scroll : ContainerScroll.Config.NONE, widget.getWidth(), widget.getHeight()));
            } catch (WidgetConfigException exception) {
                warnings.add(exception.getMessage());
            } catch (RuntimeException exception) {
                AnchorUI.LOGGER.error("Failed to create Anchor UI HUD widget '{}' (type '{}')", definition.id(), element.type, exception);
                warnings.add("Failed to create HUD widget '" + definition.id() + "': " + exception.getClass().getSimpleName());
            }
        }

        widgets = List.copyOf(created);
        relayout(warnings);
        for (String warning : warnings) {
            AnchorUI.LOGGER.warn("Anchor UI HUD: {}", warning);
        }
    }

    private void relayout(List<String> warnings) {
        if (builtWidth <= 0 || builtHeight <= 0) {
            return;
        }
        try {
            Map<String, AnchorLayout.Bounds> bounds = resolveLayout(currentOffsets(), warnings).bounds();
            for (PlacedWidget placed : widgets) {
                AnchorLayout.Bounds frame = bounds.get(placed.id());
                placed.widget().setWidth(frame.width());
                placed.widget().setHeight(frame.height());
                placed.widget().setPosition(frame.x(), frame.y());
            }
            layoutBounds = bounds;
            Map<String, String> parents = new LinkedHashMap<>();
            Map<String, WidgetTransforms.Group> groups = new LinkedHashMap<>();
            Set<String> clippingContainers = new HashSet<>();
            for (PlacedWidget placed : widgets) {
                if (placed.containerParent() != null) {
                    parents.put(placed.id(), placed.containerParent());
                }
                if (placed.clipChildren()) {
                    clippingContainers.add(placed.id());
                }
                if (placed.group() != null) {
                    groups.put(placed.id(), placed.group());
                }
            }
            Map<String, ContainerScroll.Config> configurations = new LinkedHashMap<>();
            for (PlacedWidget placed : widgets) {
                if (placed.scroll().enabled()) {
                    configurations.put(placed.id(), placed.scroll());
                }
            }
            scrollLimits = ContainerScroll.measure(bounds, parents, configurations);
            scrollOffsets.keySet().retainAll(scrollLimits.keySet());
            for (var entry : scrollLimits.entrySet()) {
                ContainerScroll.Offset previous = scrollOffsets.getOrDefault(entry.getKey(), ContainerScroll.Offset.ZERO);
                scrollOffsets.put(entry.getKey(), entry.getValue().clamp(previous));
            }
            containerParents = Map.copyOf(parents);
            containerGroups = Map.copyOf(groups);
            this.clippingContainers = Set.copyOf(clippingContainers);
            updateScrolledTransforms();
            applyVisibility();
        } catch (IllegalArgumentException exception) {
            AnchorUI.LOGGER.error("Anchor UI HUD layout rejected ({}x{}, {} widgets): {}", builtWidth, builtHeight, widgets.size(), exception.getMessage());
            layoutBounds = Map.of();
            collapsingLayoutParents = Set.of();
            collapsedLayoutHidden = Set.of();
            clipBounds = Map.of();
            transforms = Map.of();
            scrollLimits = Map.of();
            scrollOffsets.clear();
            nativeScrollPanels.clear();
            draggedScrollPanel = null;
            widgets = List.of();
            if (warnings != null) {
                warnings.add("HUD layout rejected: " + exception.getMessage());
            }
        }
    }

    private void clear() {
        builtRevision = -1;
        builtLevel = null;
        builtWidth = -1;
        builtHeight = -1;
        widgets = List.of();
        layoutBounds = Map.of();
        clipBounds = Map.of();
        transforms = Map.of();
        scrollLimits = Map.of();
        scrollOffsets.clear();
        nativeScrollPanels.clear();
        draggedScrollPanel = null;
        containerParents = Map.of();
        containerGroups = Map.of();
        clippingContainers = Set.of();
        vanillaReferences = Map.of();
        vanillaHandles = List.of();
        vanillaSnapshot = null;
        visibilityScratch.clear();
        registry = new WidgetInstanceRegistry();
        renderFailures.clear();
    }
}
