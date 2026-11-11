package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.KeyRegistry;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.binding.BoundBoolean;
import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.layout.ContainerLayout;
import com.ludwici.anchorui.api.layout.ContainerScroll;
import com.ludwici.anchorui.api.layout.WidgetContainers;
import com.ludwici.anchorui.api.layout.WidgetTransforms;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import com.ludwici.anchorui.api.widget.AnchorWidgetRegistry;
import com.ludwici.anchorui.api.widget.WidgetConfigException;
import com.ludwici.anchorui.api.widget.WidgetContext;
import com.ludwici.anchorui.api.widget.WidgetIds;
import com.ludwici.anchorui.api.widget.WidgetInstanceRegistry;
import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.hud.AnchorHudManager;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import com.ludwici.anchorui.client.hud.HudLayoutStorage;
import net.neoforged.fml.loading.FMLPaths;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class ItemLoreScreen extends Screen {
    private record DeclaredWidget(String id, LoreEntity.UiElement element, String legacyStyle) {
    }

    private record PlacedWidget(String id, AbstractWidget widget, LoreStyle.Element style, PositionElement position, WidgetContext context, BoundBoolean visibleWhen, boolean baseVisible, String containerParent, boolean clipChildren, WidgetTransforms.Group group, ContainerLayout.AutoSize autoSize, ContainerScroll.Config scroll) {
        private AnchorLayout.Frame frame(HudLayoutOffsets offsets) {
            HudLayoutOffsets.Offset delta = offsets.get(id);
            HudLayoutOffsets.AnchorOverride anchor = offsets.anchor(id);
            PositionElement effective = anchor == null
                    ? new PositionElement(position.point, position.anchor, position.relativeTo, position.x + delta.x(), position.y + delta.y())
                    : new PositionElement(anchor.point(), anchor.anchor(), anchor.relativeTo(), position.x + delta.x(), position.y + delta.y());
            return new AnchorLayout.Frame(id, effective, widget.getWidth(), widget.getHeight());
        }
    }

    private final Screen previous;
    private final ItemStack item;
    private final LoreEntity loreEntity;
    private final ResourceLocation documentId;
    private HudLayoutOffsets savedScreenOffsets = new HudLayoutOffsets();
    private HudLayoutOffsets editingScreenOffsets;
    private boolean screenOffsetsLoaded;
    private String screenSaveError = "";
    private WidgetInstanceRegistry widgetRegistry = new WidgetInstanceRegistry();
    private List<PlacedWidget> placedWidgets = List.of();
    private final Map<String, Boolean> visibilityScratch = new HashMap<>();
    private Map<String, AnchorLayout.Bounds> layoutBounds = Map.of();
    private Map<String, AnchorLayout.Bounds> clipBounds = Map.of();
    private Map<String, WidgetTransforms.Transform> transforms = Map.of();
    private Map<String, ContainerScroll.Limits> scrollLimits = Map.of();
    private final Map<String, ContainerScroll.Offset> scrollOffsets = new HashMap<>();
    private Map<String, String> containerParents = Map.of();
    private Map<String, WidgetTransforms.Group> containerGroups = Map.of();
    private Set<String> clippingContainers = Set.of();
    private String lastLayoutFailure;
    private PlacedWidget draggingWidget;
    private String layoutError;
    private List<String> layoutWarnings = List.of();

    public ItemLoreScreen(Screen previous, ItemStack item, LoreEntity entity) {
        this(previous, item, entity, null);
    }

    public ItemLoreScreen(Screen previous, ItemStack item, LoreEntity entity, ResourceLocation documentId) {
        super(Component.empty());
        this.previous = previous;
        this.item = item;
        this.loreEntity = entity;
        this.documentId = documentId;
    }

    public record EditorHandle(String id, AnchorLayout.Bounds bounds, String relativeTo, ElementAnchor point,
                               ElementAnchor anchor, HudLayoutOffsets.Offset offset, boolean visible, boolean hiddenByUser,
                               String defaultParent, ElementAnchor defaultPoint, ElementAnchor defaultAnchor) {
        public boolean isModified(AnchorHudManager.EditableProperty property) {
            return switch (property) {
                case OFFSET -> !offset.equals(HudLayoutOffsets.Offset.ZERO);
                case PARENT -> !relativeTo.equals(defaultParent);
                case POINT -> point != defaultPoint;
                case ANCHOR -> anchor != defaultAnchor;
                case VISIBILITY -> hiddenByUser;
            };
        }
    }

    private HudLayoutOffsets currentScreenOffsets() {
        return editingScreenOffsets == null ? savedScreenOffsets : editingScreenOffsets;
    }

    private Path screenLayoutPath() {
        if (documentId == null) {
            return null;
        }
        Path root = FMLPaths.CONFIGDIR.get().resolve("anchorui").resolve("screens").resolve(documentId.getNamespace());
        Path path = root.resolve(documentId.getPath() + ".json").normalize();
        if (!path.startsWith(root.normalize())) {
            throw new IllegalArgumentException("Invalid screen layout path: " + documentId);
        }
        return path;
    }

    private void loadScreenOffsets() {
        if (screenOffsetsLoaded) {
            return;
        }
        screenOffsetsLoaded = true;
        Path path = screenLayoutPath();
        if (path == null) {
            return;
        }
        try {
            savedScreenOffsets = HudLayoutStorage.load(path);
        } catch (IOException | RuntimeException exception) {
            AnchorUI.LOGGER.warn("Could not load screen layout for {}; using resource defaults", documentId, exception);
            savedScreenOffsets = new HudLayoutOffsets();
        }
    }

    public void beginScreenEditing() {
        loadScreenOffsets();
        if (editingScreenOffsets == null) {
            editingScreenOffsets = savedScreenOffsets.copy();
        }
    }

    public boolean isScreenEditing() {
        return editingScreenOffsets != null;
    }

    public String editorNamespace() {
        return documentId == null ? loreEntity.namespace : documentId.getNamespace();
    }

    public String screenSaveError() {
        return screenSaveError;
    }

    public boolean saveScreenEditing() {
        if (editingScreenOffsets == null) {
            return true;
        }
        Path path = screenLayoutPath();
        if (path == null) {
            screenSaveError = "Screen needs a resource ID for saving";
            return false;
        }
        try {
            HudLayoutStorage.save(path, editingScreenOffsets);
            savedScreenOffsets = editingScreenOffsets.copy();
            editingScreenOffsets = null;
            screenSaveError = "";
            return true;
        } catch (IOException | RuntimeException exception) {
            screenSaveError = "Save failed: " + exception.getMessage();
            AnchorUI.LOGGER.error("Could not save Anchor UI screen layout {}", documentId, exception);
            return false;
        }
    }

    public void cancelScreenEditing() {
        if (editingScreenOffsets != null) {
            editingScreenOffsets = null;
            applyScreenOffsets(savedScreenOffsets);
        }
    }

    public List<EditorHandle> editorHandles() {
        List<EditorHandle> handles = new ArrayList<>();
        HudLayoutOffsets offsets = currentScreenOffsets();
        for (PlacedWidget placed : placedWidgets) {
            AnchorLayout.Bounds bounds = layoutBounds.get(placed.id());
            if (bounds == null) {
                continue;
            }
            HudLayoutOffsets.AnchorOverride override = offsets.anchor(placed.id());
            PositionElement p = placed.position();
            String parent = override == null ? p.relativeTo : override.relativeTo();
            ElementAnchor point = override == null ? p.point : override.point();
            ElementAnchor anchor = override == null ? p.anchor : override.anchor();
            WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
            handles.add(new EditorHandle(placed.id(), transform.physical(bounds), parent, point, anchor,
                    offsets.get(placed.id()), placed.widget().visible, offsets.isHidden(placed.id()),
                    p.relativeTo, p.point, p.anchor));
        }
        return List.copyOf(handles);
    }

    public double editorDragScale(String id) {
        WidgetTransforms.Transform transform = transforms.getOrDefault(id, WidgetTransforms.IDENTITY);
        for (PlacedWidget placed : placedWidgets) {
            if (placed.id().equals(id)) {
                return transform.scale() / (placed.group() == null ? 1.0 : placed.group().scale());
            }
        }
        return 1.0;
    }

    public boolean editorOffset(String id, int x, int y) {
        if (editingScreenOffsets == null || !layoutBounds.containsKey(id)) {
            return false;
        }
        HudLayoutOffsets candidate = editingScreenOffsets.copy();
        candidate.set(id, x, y);
        return useCandidate(candidate);
    }

    public boolean editorHidden(String id, boolean hidden) {
        if (editingScreenOffsets == null || !layoutBounds.containsKey(id)) {
            return false;
        }
        HudLayoutOffsets candidate = editingScreenOffsets.copy();
        candidate.setHidden(id, hidden);
        return useCandidate(candidate);
    }

    public boolean editorAnchor(String id, String parent, ElementAnchor point, ElementAnchor anchor) {
        if (editingScreenOffsets == null || !layoutBounds.containsKey(id) || id.equals(parent)
                || (!"screen".equals(parent) && !layoutBounds.containsKey(parent))) {
            return false;
        }
        HudLayoutOffsets candidate = editingScreenOffsets.copy();
        PlacedWidget original = null;
        for (PlacedWidget placed : placedWidgets) {
            if (placed.id().equals(id)) {
                original = placed;
                break;
            }
        }
        if (original == null) {
            return false;
        }
        PositionElement p = original.position();
        if (p.relativeTo.equals(parent) && p.point == point && p.anchor == anchor) {
            candidate.clearAnchor(id);
        } else {
            candidate.setAnchor(id, new HudLayoutOffsets.AnchorOverride(parent, point, anchor));
        }
        return useCandidate(candidate);
    }

    public JsonObject editorSourceSettings(String id) {
        return widgetRegistry.definition(id).map(definition -> definition.settings.deepCopy()).orElse(null);
    }

    public boolean resetEditorProperty(String id, AnchorHudManager.EditableProperty property) {
        if (editingScreenOffsets == null) {
            return false;
        }
        EditorHandle current = null;
        for (EditorHandle handle : editorHandles()) {
            if (handle.id().equals(id)) {
                current = handle;
                break;
            }
        }
        if (current == null) {
            return false;
        }
        return switch (property) {
            case OFFSET -> editorOffset(id, 0, 0);
            case VISIBILITY -> editorHidden(id, false);
            case PARENT -> editorAnchor(id, current.defaultParent(), current.point(), current.anchor());
            case POINT -> editorAnchor(id, current.relativeTo(), current.defaultPoint(), current.anchor());
            case ANCHOR -> editorAnchor(id, current.relativeTo(), current.point(), current.defaultAnchor());
        };
    }

    public boolean resetEditorWidget(String id) {
        if (editingScreenOffsets == null) {
            return false;
        }
        HudLayoutOffsets candidate = editingScreenOffsets.copy();
        candidate.reset(id);
        return useCandidate(candidate);
    }

    public boolean resetAllEditorWidgets() {
        if (editingScreenOffsets == null) {
            return false;
        }
        HudLayoutOffsets candidate = editingScreenOffsets.copy();
        candidate.resetAll();
        return useCandidate(candidate);
    }

    private boolean useCandidate(HudLayoutOffsets candidate) {
        if (!applyScreenOffsets(candidate)) {
            return false;
        }
        editingScreenOffsets = candidate;
        return true;
    }

    private boolean applyScreenOffsets(HudLayoutOffsets offsets) {
        return applyScreenOffsets(offsets, null);
    }

    private boolean applyScreenOffsets(HudLayoutOffsets offsets, List<String> warnings) {
        if (placedWidgets.isEmpty()) {
            lastLayoutFailure = "No widgets available";
            return false;
        }
        try {
            List<AnchorLayout.Frame> frames = new ArrayList<>();
            Map<String, PaddingElement> containers = new LinkedHashMap<>();
            Map<String, ContainerLayout.AutoSize> autoSizes = new LinkedHashMap<>();
            Map<String, String> parents = new LinkedHashMap<>();
            Map<String, WidgetTransforms.Group> groups = new LinkedHashMap<>();
            Map<String, ContainerScroll.Config> scrollConfigs = new LinkedHashMap<>();
            Set<String> clipping = new HashSet<>();
            for (PlacedWidget placed : placedWidgets) {
                frames.add(placed.frame(offsets));
                if (placed.widget() instanceof PanelWidget) {
                    containers.put(placed.id(), placed.style().padding);
                    if (placed.autoSize().enabled()) {
                        autoSizes.put(placed.id(), placed.autoSize());
                    }
                    if (placed.scroll().enabled()) {
                        scrollConfigs.put(placed.id(), placed.scroll());
                    }
                }
                if (placed.containerParent() != null) {
                    parents.put(placed.id(), placed.containerParent());
                }
                if (placed.clipChildren()) {
                    clipping.add(placed.id());
                }
                if (placed.group() != null) {
                    groups.put(placed.id(), placed.group());
                }
            }
            ContainerLayout.Result result = ContainerLayout.resolve(width, height, frames, containers, autoSizes, parents,
                    warning -> {
                        if (warnings != null) {
                            warnings.add(warning);
                        }
                    });
            Map<String, AnchorLayout.Bounds> bounds = result.bounds();
            Map<String, ContainerScroll.Limits> limits = ContainerScroll.measure(bounds, parents, scrollConfigs);
            scrollOffsets.keySet().retainAll(limits.keySet());
            for (var entry : limits.entrySet()) {
                ContainerScroll.Offset previous = scrollOffsets.getOrDefault(entry.getKey(), ContainerScroll.Offset.ZERO);
                scrollOffsets.put(entry.getKey(), entry.getValue().clamp(previous));
            }
            for (PlacedWidget placed : placedWidgets) {
                AnchorLayout.Bounds frame = bounds.get(placed.id());
                placed.widget().setWidth(frame.width());
                placed.widget().setHeight(frame.height());
                placed.widget().setPosition(frame.x(), frame.y());
            }
            layoutBounds = bounds;
            scrollLimits = limits;
            containerParents = Map.copyOf(parents);
            containerGroups = Map.copyOf(groups);
            clippingContainers = Set.copyOf(clipping);
            updateScrolledTransforms();
            lastLayoutFailure = null;
            return true;
        } catch (IllegalArgumentException exception) {
            lastLayoutFailure = exception.getMessage();
            AnchorUI.LOGGER.warn("Invalid Anchor UI layout for {}: {}", documentId, lastLayoutFailure);
            return false;
        }
    }

    private void updateScrolledTransforms() {
        transforms = WidgetTransforms.resolve(layoutBounds, containerParents, containerGroups, scrollOffsets);
        clipBounds = WidgetContainers.clips(layoutBounds, containerParents, clippingContainers, transforms);
    }

    public boolean scrollContentsAt(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (int index = placedWidgets.size() - 1; index >= 0; index--) {
            PlacedWidget placed = placedWidgets.get(index);
            if (!placed.widget().visible || !placed.scroll().enabled() || !insideClip(placed, mouseX, mouseY)) {
                continue;
            }
            AnchorLayout.Bounds content = layoutBounds.get(WidgetContainers.contentId(placed.id()));
            if (content == null) {
                continue;
            }
            WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
            AnchorLayout.Bounds viewport = transform.physical(content);
            if (!WidgetContainers.contains(viewport, mouseX, mouseY)) {
                continue;
            }
            ContainerScroll.Limits limits = scrollLimits.get(placed.id());
            if (limits == null) {
                continue;
            }
            ContainerScroll.Offset old = scrollOffsets.getOrDefault(placed.id(), ContainerScroll.Offset.ZERO);
            ContainerScroll.Offset next = ContainerScroll.move(old, limits, placed.scroll(), scrollX, scrollY);
            if (!next.equals(old)) {
                scrollOffsets.put(placed.id(), next);
                updateScrolledTransforms();
                return true;
            }
        }
        return false;
    }

    private void renderScrollIndicators(GuiGraphics graphics) {
        for (PlacedWidget placed : placedWidgets) {
            if (!placed.widget().visible || !placed.scroll().enabled()) {
                continue;
            }
            ContainerScroll.Limits limits = scrollLimits.get(placed.id());
            AnchorLayout.Bounds content = layoutBounds.get(WidgetContainers.contentId(placed.id()));
            if (limits == null || content == null || (limits.x() == 0 && limits.y() == 0)) {
                continue;
            }
            WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
            AnchorLayout.Bounds viewport = transform.physical(content);
            AnchorLayout.Bounds clip = clipBounds.get(placed.id());
            if (clip != null && (clip.width() == 0 || clip.height() == 0)) {
                continue;
            }
            if (clip != null) {
                graphics.enableScissor(clip.x(), clip.y(), clip.x() + clip.width(), clip.y() + clip.height());
            }
            try {
                ContainerScroll.Offset offset = scrollOffsets.getOrDefault(placed.id(), ContainerScroll.Offset.ZERO);
                if (limits.y() > 0 && viewport.height() > 4) {
                    int track = viewport.height();
                    int thumb = Math.max(7, (int) Math.round((double) track * content.height()
                            / (content.height() + limits.y())));
                    thumb = Math.min(track, thumb);
                    int top = viewport.y() + (track - thumb) * offset.y() / limits.y();
                    graphics.fill(viewport.x() + viewport.width() - 3, viewport.y(),
                            viewport.x() + viewport.width() - 1, viewport.y() + viewport.height(), 0x88415A70);
                    graphics.fill(viewport.x() + viewport.width() - 3, top,
                            viewport.x() + viewport.width() - 1, top + thumb, 0xFFD5DFED);
                }
                if (limits.x() > 0 && viewport.width() > 4) {
                    int track = viewport.width();
                    int thumb = Math.max(7, (int) Math.round((double) track * content.width()
                            / (content.width() + limits.x())));
                    thumb = Math.min(track, thumb);
                    int left = viewport.x() + (track - thumb) * offset.x() / limits.x();
                    graphics.fill(viewport.x(), viewport.y() + viewport.height() - 3,
                            viewport.x() + viewport.width(), viewport.y() + viewport.height() - 1, 0x88415A70);
                    graphics.fill(left, viewport.y() + viewport.height() - 3,
                            left + thumb, viewport.y() + viewport.height() - 1, 0xFFD5DFED);
                }
            } finally {
                if (clip != null) {
                    graphics.disableScissor();
                }
            }
        }
    }

    @Override
    protected void init() {
        loadScreenOffsets();
        widgetRegistry = new WidgetInstanceRegistry();
        placedWidgets = List.of();
        layoutBounds = Map.of();
        clipBounds = Map.of();
        transforms = Map.of();
        scrollLimits = Map.of();
        scrollOffsets.clear();
        draggingWidget = null;
        layoutError = null;
        layoutWarnings = List.of();

        LoreStyle theme = LoreLoader.INSTANCE.getStyle(loreEntity.style);
        List<DeclaredWidget> definitions = new ArrayList<>();
        try {
            if (!loreEntity.hasExplicitWidgets) {
                declare(definitions, legacy(WidgetTypeIds.TEXT, "title", loreEntity.title, 1.0F), "title");
                declare(definitions, legacy(WidgetTypeIds.MODEL, "model", "", loreEntity.scale), "model");
                declare(definitions, legacy(WidgetTypeIds.LIST, "lore", "", 1.0F), "lore");
            } else {
                int index = 0;
                for (LoreEntity.UiElement element : loreEntity.entities) {
                    String defaultId = element.type.getPath().replace('/', '_') + "_" + index;
                    String id = element.id.isBlank() ? defaultId : element.id;
                    declare(definitions, element, id, null);
                    index++;
                }
            }
        } catch (IllegalArgumentException exception) {
            layoutError = exception.getMessage();
            AnchorUI.LOGGER.error("Anchor UI widget declaration rejected: {}", layoutError);
            return;
        }

        WidgetContext context = new WidgetContext(minecraft, this, item, loreEntity, widgetRegistry);
        List<PlacedWidget> created = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (DeclaredWidget definition : definitions) {
            LoreEntity.UiElement element = definition.element();
            try {
                LoreStyle styleSource = element.settings.has("style") ? LoreLoader.INSTANCE.getStyle(element.settings.get("style").getAsString()) : theme;
                LoreStyle.Element style = definition.legacyStyle() == null ? styleSource.get(element.type, element.settings) : styleSource.get(definition.legacyStyle(), null);
                createWidget(context, definition.id(), element, style, created, warnings);
            } catch (RuntimeException exception) {
                AnchorUI.LOGGER.error("Failed to configure Anchor UI widget '{}'", definition.id(), exception);
                warnings.add("Invalid settings for widget '" + definition.id() + "': " + exception.getMessage());
            }
        }

        placedWidgets = List.copyOf(created);
        if (!applyScreenOffsets(currentScreenOffsets(), warnings)) {
            if (currentScreenOffsets().sameAs(new HudLayoutOffsets()) || !applyScreenOffsets(new HudLayoutOffsets(), warnings)) {
                layoutError = lastLayoutFailure;
                AnchorUI.LOGGER.error("Anchor UI screen layout rejected ({}x{}): {}", width, height, layoutError);
            } else {
                AnchorUI.LOGGER.warn("Ignoring invalid saved screen layout for {}", documentId);
            }
        }
        layoutWarnings = List.copyOf(warnings);
        for (String warning : warnings) {
            AnchorUI.LOGGER.warn("Anchor UI layout: {}", warning);
        }
        if (layoutError != null) {
            return;
        }
        for (PlacedWidget placed : created) {
            addWidget(placed.widget());
        }
    }

    private void declare(List<DeclaredWidget> definitions, LoreEntity.UiElement element, String legacyStyle) {
        declare(definitions, element, element.id, legacyStyle);
    }

    private void declare(List<DeclaredWidget> definitions, LoreEntity.UiElement element, String id, String legacyStyle) {
        String qualified = WidgetIds.id(id, loreEntity.namespace);
        widgetRegistry.declare(qualified, element);
        definitions.add(new DeclaredWidget(qualified, element, legacyStyle));
    }

    private static LoreEntity.UiElement legacy(ResourceLocation type, String id, String text, float scale) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.toString());
        json.addProperty("id", id);
        json.addProperty("text", text);
        json.addProperty("scale", scale);
        return new LoreEntity.UiElement(json);
    }

    private void createWidget(WidgetContext context, String id, LoreEntity.UiElement element, LoreStyle.Element settings, List<PlacedWidget> created, List<String> warnings) {
        var factory = AnchorWidgetRegistry.find(element.type).orElse(null);
        if (factory == null) {
            warnings.add("Unknown widget type '" + element.type + "' on element '" + id + "'; widget skipped");
            return;
        }

        AbstractWidget widget;
        try {
            widget = factory.create(context, element, settings);
        } catch (WidgetConfigException exception) {
            AnchorUI.LOGGER.warn("{}", exception.getMessage());
            warnings.add(exception.getMessage());
            return;
        } catch (RuntimeException exception) {
            AnchorUI.LOGGER.error("Anchor UI widget factory failed: type='{}', id='{}'", element.type, id, exception);
            warnings.add("Widget factory failed for '" + id + "' (" + element.type + "): " + exception.getClass().getSimpleName());
            return;
        }
        if (widget == null) {
            warnings.add("Widget factory returned null: type='" + element.type + "', id='" + id + "'");
            return;
        }

        if (settings.size.width > 0) {
            widget.setWidth(settings.size.width);
        }
        if (settings.size.height > 0) {
            widget.setHeight(settings.size.height);
        }
        widget.visible = widget.visible && !settings.hidden;
        PositionElement p = settings.position;
        String relativeTo = WidgetIds.reference(p.relativeTo, loreEntity.namespace);
        PositionElement position = new PositionElement(p.point, p.anchor, relativeTo, p.x, p.y);
        widgetRegistry.attach(id, widget);
        boolean panel = widget instanceof PanelWidget;
        created.add(new PlacedWidget(id, widget, settings, position, context, BoundBoolean.parse(element.settings, "visibleWhen", true),
                widget.visible, WidgetContainers.parent(element.settings),
                panel && (WidgetContainers.clipChildren(element.settings) || settings.scroll.enabled()),
                panel ? WidgetContainers.group(element.settings) : null,
                panel ? settings.autoSize : ContainerLayout.AutoSize.NONE,
                panel ? settings.scroll : ContainerScroll.Config.NONE));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        visibilityScratch.clear();
        Map<String, Boolean> visible = visibilityScratch;
        HudLayoutOffsets overrides = currentScreenOffsets();
        for (PlacedWidget placed : placedWidgets) {
            boolean parentVisible = placed.containerParent() == null || visible.getOrDefault(placed.containerParent(), false);
            boolean shown = parentVisible && placed.baseVisible() && !overrides.isHidden(placed.id()) && placed.visibleWhen().get(placed.context());
            placed.widget().visible = shown;
            visible.put(placed.id(), shown);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
        for (PlacedWidget placed : placedWidgets) {
            AbstractWidget widget = placed.widget();
            if (!widget.visible) {
                continue;
            }
            AnchorLayout.Bounds clip = clipBounds.get(placed.id());
            if (clip != null && (clip.width() == 0 || clip.height() == 0)) {
                continue;
            }
            if (clip != null) {
                graphics.enableScissor(clip.x(), clip.y(), clip.x() + clip.width(), clip.y() + clip.height());
            }
            try {
                int color = placed.style().background.color;
                if (!(widget instanceof AnchoredWidget) && (color >>> 24) != 0) {
                    graphics.fill(widget.getX(), widget.getY(), widget.getRight(), widget.getBottom(), color);
                }
                WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
                boolean mouseInside = clip == null || WidgetContainers.contains(clip, mouseX, mouseY);
                int widgetMouseX = mouseInside ? (int) Math.floor(transform.inverseX(mouseX)) : Integer.MIN_VALUE;
                int widgetMouseY = mouseInside ? (int) Math.floor(transform.inverseY(mouseY)) : Integer.MIN_VALUE;
                graphics.pose().pushPose();
                try {
                    graphics.pose().translate(transform.translateX(), transform.translateY(), 0);
                    graphics.pose().scale((float) transform.scale(), (float) transform.scale(), 1.0F);
                    widget.render(graphics, widgetMouseX, widgetMouseY, partialTick);
                } finally {
                    graphics.pose().popPose();
                }
            } finally {
                if (clip != null) {
                    graphics.disableScissor();
                }
            }
        }
        renderScrollIndicators(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        draggingWidget = null;
        for (int index = placedWidgets.size() - 1; index >= 0; index--) {
            PlacedWidget placed = placedWidgets.get(index);
            AbstractWidget widget = placed.widget();
            if (!widget.visible || !widget.active || !insideClip(placed, mouseX, mouseY)) {
                continue;
            }
            WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
            if (widget.mouseClicked(transform.inverseX(mouseX), transform.inverseY(mouseY), button)) {
                setFocused(widget);
                draggingWidget = placed;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingWidget == null) {
            return false;
        }
        WidgetTransforms.Transform transform = transforms.getOrDefault(draggingWidget.id(), WidgetTransforms.IDENTITY);
        return draggingWidget.widget().mouseDragged(transform.inverseX(mouseX), transform.inverseY(mouseY), button,
                dragX / transform.scale(), dragY / transform.scale());
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingWidget == null) {
            return false;
        }
        PlacedWidget placed = draggingWidget;
        draggingWidget = null;
        WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
        return placed.widget().mouseReleased(transform.inverseX(mouseX), transform.inverseY(mouseY), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (int index = placedWidgets.size() - 1; index >= 0; index--) {
            PlacedWidget placed = placedWidgets.get(index);
            if (!placed.widget().visible || !insideClip(placed, mouseX, mouseY)) {
                continue;
            }
            WidgetTransforms.Transform transform = transforms.getOrDefault(placed.id(), WidgetTransforms.IDENTITY);
            AnchorLayout.Bounds frame = layoutBounds.get(placed.id());
            if (frame != null && WidgetContainers.contains(transform.physical(frame), mouseX, mouseY)
                    && placed.widget().mouseScrolled(transform.inverseX(mouseX), transform.inverseY(mouseY), scrollX, scrollY)) {
                return true;
            }
        }
        return scrollContentsAt(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean insideClip(PlacedWidget placed, double mouseX, double mouseY) {
        AnchorLayout.Bounds clip = clipBounds.get(placed.id());
        return clip == null || WidgetContainers.contains(clip, mouseX, mouseY);
    }

    protected final WidgetInstanceRegistry getWidgetRegistry() {
        return widgetRegistry;
    }

    protected final Map<String, AnchorLayout.Bounds> getLayoutBounds() {
        return layoutBounds;
    }

    protected final String getLayoutError() {
        return layoutError;
    }

    protected final List<String> getLayoutWarnings() {
        return layoutWarnings;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(previous);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyRegistry.EDIT_HUD.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            minecraft.setScreen(new AnchorScreenEditorScreen(this));
            return true;
        }
        if (minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
