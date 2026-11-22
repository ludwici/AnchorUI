package com.ludwici.anchorui;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyRegistry {
    private static final String CATEGORY = "key.categories.anchorui";

    public static final KeyMapping SHOW_LORE = new KeyMapping("key." + AnchorUI.MODID + ".show_lore", GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping EDIT_HUD = new KeyMapping("key." + AnchorUI.MODID + ".edit_hud", GLFW.GLFW_KEY_F8, CATEGORY);

    private KeyRegistry() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(SHOW_LORE);
        KeyBindingHelper.registerKeyBinding(EDIT_HUD);
    }
}
