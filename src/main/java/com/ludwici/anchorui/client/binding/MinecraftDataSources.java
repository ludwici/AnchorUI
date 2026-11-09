package com.ludwici.anchorui.client.binding;

import com.ludwici.anchorui.api.binding.NumericDataRegistry;
import net.minecraft.resources.ResourceLocation;

public final class MinecraftDataSources {
    private MinecraftDataSources() {
    }

    public static void register() {
        register("player.health", ctx -> ctx.minecraft().player == null ? 0.0 : ctx.minecraft().player.getHealth());
        register("player.max_health", ctx -> ctx.minecraft().player == null ? 20.0 : ctx.minecraft().player.getMaxHealth());
        register("player.food", ctx -> ctx.minecraft().player == null ? 0.0 : ctx.minecraft().player.getFoodData().getFoodLevel());
        register("player.experience", ctx -> ctx.minecraft().player == null ? 0.0 : ctx.minecraft().player.experienceProgress);
        register("player.level", ctx -> ctx.minecraft().player == null ? 0.0 : ctx.minecraft().player.experienceLevel);
    }

    private static void register(String path, NumericDataRegistry.Provider provider) {
        NumericDataRegistry.register(ResourceLocation.fromNamespaceAndPath("anchorui", path), provider);
    }
}
