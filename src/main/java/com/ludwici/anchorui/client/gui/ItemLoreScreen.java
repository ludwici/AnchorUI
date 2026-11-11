package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.binding.BoundBoolean;
import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.layout.WidgetContainers;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.style.PositionElement;
import com.ludwici.anchorui.api.widget.AnchorWidgetRegistry;
import com.ludwici.anchorui.api.widget.WidgetConfigException;
import com.ludwici.anchorui.api.widget.WidgetContext;
import com.ludwici.anchorui.api.widget.WidgetIds;
import com.ludwici.anchorui.api.widget.WidgetInstanceRegistry;
import com.ludwici.anchorui.client.LoreLoader;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

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

    private record PlacedWidget(String id, AbstractWidget widget, LoreStyle.Element style, PositionElement position, WidgetContext context, BoundBoolean visibleWhen, boolean baseVisible, String containerParent, boolean clipChildren) {
        private AnchorLayout.Frame frame() {
            return new AnchorLayout.Frame(id, position, widget.getWidth(), widget.getHeight());
        }
    }

    private final Screen previous;
    private final ItemStack item;
    private final LoreEntity loreEntity;
    private WidgetInstanceRegistry widgetRegistry = new WidgetInstanceRegistry();
    private List<PlacedWidget> placedWidgets = List.of();
    private Map<String, AnchorLayout.Bounds> layoutBounds = Map.of();
    private Map<String, AnchorLayout.Bounds> clipBounds = Map.of();
    private String layoutError;
    private List<String> layoutWarnings = List.of();

    public ItemLoreScreen(Screen previous, ItemStack item, LoreEntity entity) {
        super(Component.empty());
        this.previous = previous;
        this.item = item;
        this.loreEntity = entity;
    }

    @Override
    protected void init() {
        widgetRegistry = new WidgetInstanceRegistry();
        placedWidgets = List.of();
        layoutBounds = Map.of();
        clipBounds = Map.of();
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

        try {
            List<AnchorLayout.Frame> frames = new ArrayList<>();
            for (PlacedWidget placed : created) {
                frames.add(placed.frame());
                if (placed.widget() instanceof PanelWidget) {
                    frames.add(WidgetContainers.contentFrame(placed.id(), placed.widget().getWidth(), placed.widget().getHeight(), placed.style().padding));
                }
            }
            layoutBounds = AnchorLayout.resolve(width, height, frames, warnings::add);
            Map<String, String> parents = new LinkedHashMap<>();
            Set<String> clippingContainers = new HashSet<>();
            for (PlacedWidget placed : created) {
                if (placed.containerParent() != null) {
                    parents.put(placed.id(), placed.containerParent());
                }
                if (placed.clipChildren()) {
                    clippingContainers.add(placed.id());
                }
            }
            clipBounds = WidgetContainers.clips(layoutBounds, parents, clippingContainers);
            for (PlacedWidget placed : created) {
                AnchorLayout.Bounds bounds = layoutBounds.get(placed.id());
                placed.widget().setPosition(bounds.x(), bounds.y());
            }
        } catch (IllegalArgumentException exception) {
            layoutError = exception.getMessage();
            AnchorUI.LOGGER.error("Anchor UI layout rejected ({}x{}, {} widgets): {}", width, height, created.size(), layoutError);
        }

        layoutWarnings = List.copyOf(warnings);
        for (String warning : warnings) {
            AnchorUI.LOGGER.warn("Anchor UI layout: {}", warning);
        }
        if (layoutError != null) {
            return;
        }
        placedWidgets = List.copyOf(created);
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
        created.add(new PlacedWidget(id, widget, settings, position, context, BoundBoolean.parse(element.settings, "visibleWhen", true), widget.visible, WidgetContainers.parent(element.settings), widget instanceof PanelWidget && WidgetContainers.clipChildren(element.settings)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Map<String, Boolean> visible = new HashMap<>();
        for (PlacedWidget placed : placedWidgets) {
            boolean parentVisible = placed.containerParent() == null || visible.getOrDefault(placed.containerParent(), false);
            boolean shown = parentVisible && placed.baseVisible() && placed.visibleWhen().get(placed.context());
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
                int widgetMouseX = clip == null || WidgetContainers.contains(clip, mouseX, mouseY) ? mouseX : -1;
                int widgetMouseY = widgetMouseX == -1 ? -1 : mouseY;
                widget.render(graphics, widgetMouseX, widgetMouseY, partialTick);
            } finally {
                if (clip != null) {
                    graphics.disableScissor();
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Map<AbstractWidget, Boolean> temporarilyInactive = new HashMap<>();
        for (PlacedWidget placed : placedWidgets) {
            AnchorLayout.Bounds clip = clipBounds.get(placed.id());
            if (clip != null && !WidgetContainers.contains(clip, mouseX, mouseY)) {
                AbstractWidget widget = placed.widget();
                temporarilyInactive.put(widget, widget.active);
                widget.active = false;
            }
        }
        try {
            return super.mouseClicked(mouseX, mouseY, button);
        } finally {
            for (var entry : temporarilyInactive.entrySet()) {
                entry.getKey().active = entry.getValue();
            }
        }
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
        if (minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, scanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
