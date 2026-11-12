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
import com.ludwici.anchorui.client.hud.AnchorHudManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import com.ludwici.anchorui.client.test.TestWidgetRegistration;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

import static com.ludwici.anchorui.AnchorUI.MODID;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class Events {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MinecraftDataSources.register();
            BuiltinDataFormatters.register();
            ClientUiActions.register();
            AnchorWidgetRegistry.register(WidgetTypeIds.BUTTON, JsonActionButton::create);
            AnchorWidgetRegistry.register(WidgetTypeIds.PROGRESS, (context, element, style) -> new ProgressWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.DATA_LIST, (context, element, style) -> new DataListWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.TEMPLATE_LIST, (context, element, style) -> new PanelWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.TEXT, (context, element, style) -> {
                BoundText binding = BoundText.parse(element.settings.get("text"));
                return binding.isDynamic() ? new BoundTextWidget(context, binding, style) : new AnchoredTextWidget(element.text, context.font(), style);
            });
            AnchorWidgetRegistry.register(WidgetTypeIds.MODEL, (context, element, style) -> new ItemModelWidget(context.resolveItem(element.path), element.scale, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.IMAGE, (context, element, style) -> new ImageWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.PANEL, (context, element, style) -> new PanelWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.CONTAINER, (context, element, style) -> new PanelWidget(context, element.settings, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.LIST, (context, element, style) -> new ItemLoreListWidget(element.lore.isEmpty() ? context.entity().components : element.lore, context.font(), style));
            TestWidgetRegistration.register();
        });
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CHAT, ResourceLocation.fromNamespaceAndPath(MODID, "hud"), AnchorHudManager.INSTANCE::render);
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(LoreLoader.INSTANCE);
    }
}
