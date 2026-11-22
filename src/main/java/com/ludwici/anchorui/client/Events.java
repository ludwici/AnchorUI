package com.ludwici.anchorui.client;

import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.widget.AnchorWidgetRegistry;
import com.ludwici.anchorui.client.gui.AnchoredTextWidget;
import com.ludwici.anchorui.client.gui.BoundTextWidget;
import com.ludwici.anchorui.client.gui.DataListWidget;
import com.ludwici.anchorui.api.binding.BoundText;
import com.ludwici.anchorui.api.binding.BuiltinDataFormatters;
import com.ludwici.anchorui.client.gui.ItemLoreListWidget;
import com.ludwici.anchorui.client.gui.ItemModelWidget;
import com.ludwici.anchorui.client.gui.ImageWidget;
import com.ludwici.anchorui.client.gui.PanelWidget;
import com.ludwici.anchorui.client.gui.ProgressWidget;
import com.ludwici.anchorui.client.binding.MinecraftDataSources;
import com.ludwici.anchorui.client.action.ClientUiActions;
import com.ludwici.anchorui.client.gui.JsonActionButton;
import com.ludwici.anchorui.client.gui.StateControlWidget;
import com.ludwici.anchorui.client.gui.ColorPickerWidget;
import com.ludwici.anchorui.client.hud.AnchorHudManager;
import net.minecraft.resources.ResourceLocation;
import com.ludwici.anchorui.client.test.TestWidgetRegistration;



public class Events {
    public static void initialize() {
            MinecraftDataSources.register();
            BuiltinDataFormatters.register();
            ClientUiActions.register();
            AnchorWidgetRegistry.register(WidgetTypeIds.BUTTON, JsonActionButton::create);
            AnchorWidgetRegistry.register(WidgetTypeIds.CHECKBOX, (context, element, style) -> StateControlWidget.create(context, element, style, StateControlWidget.Kind.CHECKBOX));
            AnchorWidgetRegistry.register(WidgetTypeIds.SWITCH, (context, element, style) -> StateControlWidget.create(context, element, style, StateControlWidget.Kind.SWITCH));
            AnchorWidgetRegistry.register(WidgetTypeIds.SELECT, (context, element, style) -> StateControlWidget.create(context, element, style, StateControlWidget.Kind.SELECT));
            AnchorWidgetRegistry.register(WidgetTypeIds.TABS, (context, element, style) -> StateControlWidget.create(context, element, style, StateControlWidget.Kind.TABS));
            AnchorWidgetRegistry.register(WidgetTypeIds.COLOR_PICKER, ColorPickerWidget::create);
            AnchorWidgetRegistry.register(WidgetTypeIds.PROGRESS, (context, element, style) -> new ProgressWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.DATA_LIST, (context, element, style) -> new DataListWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.TEMPLATE_LIST, (context, element, style) -> new PanelWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.TEXT, (context, element, style) -> {
                BoundText binding = BoundText.parse(element.settings.get("text"));
                return binding.isDynamic() ? new BoundTextWidget(context, binding, style)
                        : new AnchoredTextWidget(element.text, context.font(), style, context.screen());
            });
            AnchorWidgetRegistry.register(WidgetTypeIds.MODEL, (context, element, style) -> new ItemModelWidget(context.resolveItem(element.path), element.scale, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.IMAGE, (context, element, style) -> new ImageWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.PANEL, (context, element, style) -> new PanelWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.CONTAINER, (context, element, style) -> new PanelWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.LIST, (context, element, style) -> new ItemLoreListWidget(element.lore.isEmpty() ? context.entity().components : element.lore, context.font(), style));
            TestWidgetRegistration.register();
    }
}
