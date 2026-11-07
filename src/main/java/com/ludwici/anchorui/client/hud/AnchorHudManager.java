package com.ludwici.anchorui.client.hud;

import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import com.ludwici.anchorui.api.widget.AnchorWidgetRegistry;
import com.ludwici.anchorui.api.widget.WidgetConfigException;
import com.ludwici.anchorui.api.widget.WidgetContext;
import com.ludwici.anchorui.api.widget.WidgetIds;
import com.ludwici.anchorui.api.widget.WidgetInstanceRegistry;
import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.gui.AnchoredWidget;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AnchorHudManager {
    public static final AnchorHudManager INSTANCE = new AnchorHudManager();

    public record HudHandle(String id, AnchorLayout.Bounds bounds, String relativeTo, ElementAnchor point, ElementAnchor anchor, HudLayoutOffsets.Offset offset, boolean visible, boolean hiddenByUser) {
    }

    private record Description(String id, LoreEntity entity, LoreEntity.UiElement element) {
    }

    private record PlacedWidget(String id, AbstractWidget widget, LoreStyle.Element style, PositionElement position, boolean baseVisible) {
        private PositionElement effectivePosition(HudLayoutOffsets offsets) {
            HudLayoutOffsets.Offset delta = offsets.get(id);
            HudLayoutOffsets.AnchorOverride anchor = offsets.anchor(id);
            ElementAnchor point = anchor == null ? position.point : anchor.point();
            ElementAnchor targetPoint = anchor == null ? position.anchor : anchor.anchor();
            String relativeTo = anchor == null ? position.relativeTo : anchor.relativeTo();
            return new PositionElement(point, targetPoint, relativeTo, position.x + delta.x(), position.y + delta.y());
        }

        private AnchorLayout.Frame frame(HudLayoutOffsets offsets) {
            return new AnchorLayout.Frame(id, effectivePosition(offsets), widget.getWidth(), widget.getHeight());
        }
    }

    private WidgetInstanceRegistry registry = new WidgetInstanceRegistry();
    private List<PlacedWidget> widgets = List.of();
    private Map<String, AnchorLayout.Bounds> layoutBounds = Map.of();
    private final Set<String> renderFailures = new HashSet<>();
    private HudLayoutOffsets savedOffsets = new HudLayoutOffsets();
    private HudLayoutOffsets editingOffsets;
    private boolean storageLoaded;
    private String lastSaveError;
    private long builtRevision = -1;
    private int builtWidth = -1;
    private int builtHeight = -1;
    private ClientLevel builtLevel;
    private boolean builtTestEnabled;
    private boolean testEnabled;

    private AnchorHudManager() {
    }

    public boolean isTestEnabled() {
        return testEnabled;
    }

    public void setTestEnabled(boolean enabled) {
        if (testEnabled != enabled) {
            testEnabled = enabled;
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
        if (editingOffsets == null || !layoutBounds.containsKey(id)) {
            return;
        }
        editingOffsets.set(id, x, y);
        relayout(null);
    }

    public void togglePreviewHidden(String id) {
        if (editingOffsets == null || !layoutBounds.containsKey(id)) {
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

    public boolean cyclePreviewTargetPoint(String id) {
        HudHandle handle = handle(id);
        if (handle == null) {
            return false;
        }
        ElementAnchor[] values = ElementAnchor.values();
        ElementAnchor next = values[(handle.anchor().ordinal() + 1) % values.length];
        return changePreviewAnchor(id, new HudLayoutOffsets.AnchorOverride(handle.relativeTo(), handle.point(), next));
    }

    public boolean cyclePreviewTarget(String id) {
        HudHandle handle = handle(id);
        if (handle == null) {
            return false;
        }
        List<String> candidates = new ArrayList<>();
        candidates.add("screen");
        getHandles().stream().map(HudHandle::id).filter(candidate -> !candidate.equals(id)).sorted().forEach(candidates::add);
        int current = candidates.indexOf(handle.relativeTo());
        for (int n = 1; n <= candidates.size(); n++) {
            String candidate = candidates.get((current + n + candidates.size()) % candidates.size());
            if (changePreviewAnchor(id, new HudLayoutOffsets.AnchorOverride(candidate, handle.point(), handle.anchor()))) {
                return true;
            }
        }
        return false;
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
        AnchorLayout.Bounds before = layoutBounds.get(id);
        if (before == null) {
            return false;
        }
        HudLayoutOffsets candidate = editingOffsets.copy();
        candidate.setAnchor(id, override);
        try {
            AnchorLayout.Bounds changed = resolveBounds(candidate).get(id);
            HudLayoutOffsets.Offset original = candidate.get(id);
            candidate.set(id, original.x() + before.x() - changed.x(), original.y() + before.y() - changed.y());
            resolveBounds(candidate);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        editingOffsets = candidate;
        relayout(null);
        return true;
    }

    private Map<String, AnchorLayout.Bounds> resolveBounds(HudLayoutOffsets offsets) {
        return AnchorLayout.resolve(builtWidth, builtHeight, widgets.stream().map(placed -> placed.frame(offsets)).toList());
    }

    private void applyVisibility() {
        HudLayoutOffsets offsets = currentOffsets();
        for (PlacedWidget placed : widgets) {
            placed.widget().visible = placed.baseVisible() && !offsets.isHidden(placed.id());
        }
    }

    public void resetPreviewOffset(String id) {
        if (editingOffsets != null) {
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

    public List<HudHandle> getHandles() {
        List<HudHandle> result = new ArrayList<>();
        HudLayoutOffsets offsets = currentOffsets();
        for (PlacedWidget placed : widgets) {
            AnchorLayout.Bounds bounds = layoutBounds.get(placed.id());
            if (bounds != null && !renderFailures.contains(placed.id())) {
                PositionElement effective = placed.effectivePosition(offsets);
                result.add(new HudHandle(placed.id(), bounds, effective.relativeTo, effective.point, effective.anchor, offsets.get(placed.id()), placed.widget().visible, offsets.isHidden(placed.id())));
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
        if (minecraft.options.hideGui || (minecraft.screen != null && !(minecraft.screen instanceof ChatScreen))) {
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
        long revision = LoreLoader.INSTANCE.getRevision();
        if (builtRevision != revision || builtWidth != width || builtHeight != height || builtLevel != minecraft.level || builtTestEnabled != testEnabled) {
            rebuild(minecraft, width, height, revision);
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
        for (PlacedWidget placed : widgets) {
            if (renderFailures.contains(placed.id())) {
                continue;
            }
            AbstractWidget widget = placed.widget();
            if (!widget.visible) {
                continue;
            }
            graphics.pose().pushPose();
            try {
                int color = placed.style().background.color;
                if (!(widget instanceof AnchoredWidget) && (color >>> 24) != 0) {
                    graphics.fill(widget.getX(), widget.getY(), widget.getRight(), widget.getBottom(), color);
                }
                widget.render(graphics, -1, -1, partialTick);
            } catch (RuntimeException exception) {
                renderFailures.add(placed.id());
                AnchorUI.LOGGER.error("Anchor UI HUD widget '{}' failed during rendering; disabled until rebuild", placed.id(), exception);
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    private void rebuild(Minecraft minecraft, int width, int height, long revision) {
        builtRevision = revision;
        builtWidth = width;
        builtHeight = height;
        builtLevel = minecraft.level;
        builtTestEnabled = testEnabled;
        registry = new WidgetInstanceRegistry();
        widgets = List.of();
        layoutBounds = Map.of();
        renderFailures.clear();

        List<Description> declared = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        var documents = LoreLoader.INSTANCE.getHudDocuments().entrySet().stream().sorted(Comparator.comparing(entry -> entry.getKey().toString())).toList();
        for (var document : documents) {
            ResourceLocation source = document.getKey();
            LoreLoader.HudDocument hud = document.getValue();
            if (hud.testOnly() && !testEnabled) {
                continue;
            }
            int index = 0;
            for (LoreEntity.UiElement element : hud.entity().entities) {
                String defaultId = element.type.getPath().replace('/', '_') + "_" + index;
                try {
                    String id = WidgetIds.id(element.id.isBlank() ? defaultId : element.id, hud.entity().namespace);
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
                registry.attach(definition.id(), widget);
                created.add(new PlacedWidget(definition.id(), widget, style, anchored, widget.visible));
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
        List<AnchorLayout.Frame> frames = widgets.stream().map(placed -> placed.frame(currentOffsets())).toList();
        try {
            Map<String, AnchorLayout.Bounds> bounds = AnchorLayout.resolve(builtWidth, builtHeight, frames, warning -> {
                if (warnings != null) {
                    warnings.add(warning);
                }
            });
            for (PlacedWidget placed : widgets) {
                AnchorLayout.Bounds frame = bounds.get(placed.id());
                placed.widget().setPosition(frame.x(), frame.y());
            }
            layoutBounds = bounds;
            applyVisibility();
        } catch (IllegalArgumentException exception) {
            AnchorUI.LOGGER.error("Anchor UI HUD layout rejected ({}x{}, {} widgets): {}", builtWidth, builtHeight, widgets.size(), exception.getMessage());
            layoutBounds = Map.of();
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
        registry = new WidgetInstanceRegistry();
        renderFailures.clear();
    }
}
