package com.ludwici.anchorui.client.hud;

import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.LoreStyle;
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

    public record HudHandle(String id, AnchorLayout.Bounds bounds, String relativeTo, HudLayoutOffsets.Offset offset, boolean visible) {
    }

    private record Description(String id, LoreEntity entity, LoreEntity.UiElement element) {
    }

    private record PlacedWidget(String id, AbstractWidget widget, LoreStyle.Element style, PositionElement position) {
        private AnchorLayout.Frame frame(HudLayoutOffsets offsets) {
            HudLayoutOffsets.Offset delta = offsets.get(id);
            PositionElement applied = new PositionElement(position.point, position.anchor, position.relativeTo, position.x + delta.x(), position.y + delta.y());
            return new AnchorLayout.Frame(id, applied, widget.getWidth(), widget.getHeight());
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
                result.add(new HudHandle(placed.id(), bounds, placed.position().relativeTo, offsets.get(placed.id()), placed.widget().visible));
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
                created.add(new PlacedWidget(definition.id(), widget, style, anchored));
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
