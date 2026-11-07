package com.ludwici.anchorui.client;

import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.widget.AnchorWidgetRegistry;
import com.ludwici.anchorui.client.gui.AnchoredTextWidget;
import com.ludwici.anchorui.client.gui.ItemLoreListWidget;
import com.ludwici.anchorui.client.gui.ItemModelWidget;
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
            AnchorWidgetRegistry.register(WidgetTypeIds.TEXT, (context, element, style) -> new AnchoredTextWidget(element.text, context.font(), style));
            AnchorWidgetRegistry.register(WidgetTypeIds.MODEL, (context, element, style) -> new ItemModelWidget(context.resolveItem(element.path), element.scale, style));
            AnchorWidgetRegistry.register(WidgetTypeIds.LIST, (context, element, style) -> new ItemLoreListWidget(element.lore.isEmpty() ? context.entity().components : element.lore, context.font(), style));
            TestWidgetRegistration.register();
        });
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(LoreLoader.INSTANCE);
    }
}
