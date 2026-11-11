package com.ludwici.anchorui.client.test;

import com.ludwici.anchorui.api.action.UiActionRegistry;
import com.ludwici.anchorui.api.widget.AnchorWidgetRegistry;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.atomic.AtomicInteger;

public final class TestWidgetRegistration {
    public static final ResourceLocation COUNTER = ResourceLocation.fromNamespaceAndPath("anchorui_test", "counter");
    public static final ResourceLocation BROKEN = ResourceLocation.fromNamespaceAndPath("anchorui_test", "broken");
    public static final ResourceLocation EVENT_NOTICE = ResourceLocation.fromNamespaceAndPath("anchorui_test", "event_notice");

    private TestWidgetRegistration() {
    }

    public static void register() {
        AnchorWidgetRegistry.register(COUNTER, CounterConfig.CODEC, (context, element, style, config) -> {
            AtomicInteger clicks = new AtomicInteger(config.initial());
            return Button.builder(Component.literal("Counter: " + config.initial()), button -> button.setMessage(Component.literal("Counter: " + clicks.addAndGet(config.step())))).bounds(0, 0, 120, 20).build();
        });
        AnchorWidgetRegistry.register(BROKEN, (context, element, style) -> {
            throw new IllegalArgumentException("Intentional error from scene12 factory");
        });
        UiActionRegistry.registerEvent(EVENT_NOTICE, (context, settings) -> event -> {
            if (context.minecraft().player == null) {
                return;
            }
            String value = event.value().isJsonNull() ? "" : " = " + event.value();
            context.minecraft().player.displayClientMessage(Component.literal(
                    "Anchor UI " + event.name() + " [" + event.targetId() + "]" + value), false);
        });
    }
}
