package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.AnchorWidgetRegistry;
import com.ludwici.anchorui.api.widget.WidgetContext;
import com.ludwici.anchorui.client.LoreLoader;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemLoreScreen extends Screen {
    private record PlacedWidget(String id, AbstractWidget widget, LoreStyle.Element style) {
        private AnchorLayout.Frame frame() {
            return new AnchorLayout.Frame(id, style.position, widget.getWidth(), widget.getHeight());
        }
    }

    private final Screen previous;
    private final ItemStack item;
    private final LoreEntity loreEntity;
    private List<PlacedWidget> placedWidgets = List.of();
    private Map<String, AnchorLayout.Bounds> layoutBounds = Map.of();
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
        placedWidgets = List.of();
        layoutBounds = Map.of();
        layoutError = null;
        layoutWarnings = List.of();

        LoreStyle theme = LoreLoader.INSTANCE.getStyle(loreEntity.style);
        WidgetContext context = new WidgetContext(minecraft, this, item, loreEntity);
        List<PlacedWidget> created = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (loreEntity.entities.isEmpty()) {
            createWidget(context, legacy(WidgetTypeIds.TEXT, "title", loreEntity.title, 1.0F), theme.get("title", null), created, warnings);
            createWidget(context, legacy(WidgetTypeIds.MODEL, "model", "", loreEntity.scale), theme.get("model", null), created, warnings);
            createWidget(context, legacy(WidgetTypeIds.LIST, "lore", "", 1.0F), theme.get("lore", null), created, warnings);
        } else {
            int index = 0;
            for (LoreEntity.UiElement element : loreEntity.entities) {
                LoreStyle widgetTheme = element.settings.has("style") ? LoreLoader.INSTANCE.getStyle(element.settings.get("style").getAsString()) : theme;
                LoreStyle.Element settings = widgetTheme.get(element.type, element.settings);
                String id = element.id.isBlank() ? element.type.getPath().replace('/', '_') + "_" + index : element.id;
                createWidget(context, element, id, settings, created, warnings);
                index++;
            }
        }

        try {
            List<AnchorLayout.Frame> frames = created.stream().map(PlacedWidget::frame).toList();
            layoutBounds = AnchorLayout.resolve(width, height, frames, warnings::add);
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
            addRenderableWidget(placed.widget());
        }
    }

    private static LoreEntity.UiElement legacy(ResourceLocation type, String id, String text, float scale) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type.toString());
        json.addProperty("id", id);
        json.addProperty("text", text);
        json.addProperty("scale", scale);
        return new LoreEntity.UiElement(json);
    }

    private void createWidget(WidgetContext context, LoreEntity.UiElement element, LoreStyle.Element settings, List<PlacedWidget> created, List<String> warnings) {
        createWidget(context, element, element.id, settings, created, warnings);
    }

    private void createWidget(WidgetContext context, LoreEntity.UiElement element, String id, LoreStyle.Element settings, List<PlacedWidget> created, List<String> warnings) {
        var factory = AnchorWidgetRegistry.find(element.type).orElse(null);
        if (factory == null) {
            warnings.add("Unknown widget type '" + element.type + "' on element '" + id + "'; widget skipped");
            return;
        }

        AbstractWidget widget;
        try {
            widget = factory.create(context, element, settings);
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
        created.add(new PlacedWidget(id, widget, settings));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (PlacedWidget placed : placedWidgets) {
            AbstractWidget widget = placed.widget();
            int color = placed.style().background.color;
            if (widget.visible && !(widget instanceof AnchoredWidget) && (color >>> 24) != 0) {
                graphics.fill(widget.getX(), widget.getY(), widget.getRight(), widget.getBottom(), color);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
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
