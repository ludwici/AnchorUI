package com.ludwici.anchorui.client.test;

import com.ludwici.anchorui.api.widget.AnchorWidgetRegistry;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.concurrent.atomic.AtomicInteger;

public final class TestWidgetRegistration {
    public static final ResourceLocation COUNTER = ResourceLocation.fromNamespaceAndPath("anchorui_test", "counter");

    private TestWidgetRegistration() {
    }

    public static void register() {
        AnchorWidgetRegistry.register(COUNTER, (context, element, style) -> {
            int initial = GsonHelper.getAsInt(element.settings, "initial", 0);
            int step = GsonHelper.getAsInt(element.settings, "step", 1);
            AtomicInteger clicks = new AtomicInteger(initial);
            return Button.builder(Component.literal("Counter: " + initial), button -> button.setMessage(Component.literal("Counter: " + clicks.addAndGet(step)))).bounds(0, 0, 120, 20).build();
        });
    }
}
