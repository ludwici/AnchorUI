package com.ludwici.anchorui;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import static com.ludwici.anchorui.AnchorUI.MODID;
import static org.lwjgl.glfw.GLFW.*;

@EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyRegistry {
    private static final String CATEGORY = "key.categories.anchorui";

    public static final KeyMapping SHOW_LORE = new KeyMapping("key." + MODID + ".show_lore", GLFW_KEY_X, CATEGORY);
    public static final KeyMapping EDIT_HUD = new KeyMapping("key." + MODID + ".edit_hud", GLFW_KEY_F8, CATEGORY);

    @SubscribeEvent
    public static void onKeyBindingRegistry(RegisterKeyMappingsEvent event) {
        event.register(SHOW_LORE);
        event.register(EDIT_HUD);
    }
}
