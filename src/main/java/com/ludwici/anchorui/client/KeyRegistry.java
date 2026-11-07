package com.ludwici.anchorui.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.ludwici.anchorui.AnchorUI.MODID;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_X;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyRegistry {
    private static final String CATEGORY = "Anchor UI";

    public static final KeyMapping SHOW_LORE = new KeyMapping("key." + MODID + ".show_lore", GLFW_KEY_X, CATEGORY);

    @SubscribeEvent
    public static void onKeyBindingRegistry(RegisterKeyMappingsEvent event) {
        event.register(SHOW_LORE);
    }
}
