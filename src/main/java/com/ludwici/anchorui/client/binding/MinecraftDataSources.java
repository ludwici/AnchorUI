package com.ludwici.anchorui.client.binding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.binding.DataValueRegistry;
import com.ludwici.anchorui.api.binding.NumericDataRegistry;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;

public final class MinecraftDataSources {
    private MinecraftDataSources() {
    }

    public static void register() {
        registerNumber("player.health", ctx -> ctx.minecraft().player == null ? 0.0 : ctx.minecraft().player.getHealth());
        registerNumber("player.max_health", ctx -> ctx.minecraft().player == null ? 20.0 : ctx.minecraft().player.getMaxHealth());
        registerNumber("player.food", ctx -> ctx.minecraft().player == null ? 0.0 : ctx.minecraft().player.getFoodData().getFoodLevel());
        registerNumber("player.experience", ctx -> ctx.minecraft().player == null ? 0.0 : ctx.minecraft().player.experienceProgress);
        registerNumber("player.level", ctx -> ctx.minecraft().player == null ? 0.0 : ctx.minecraft().player.experienceLevel);
        DataValueRegistry.registerString(id("player.name"), ctx -> ctx.minecraft().player == null ? "" : ctx.minecraft().player.getName().getString());
        DataValueRegistry.registerString(id("world.dimension"), ctx -> ctx.minecraft().level == null ? "" : ctx.minecraft().level.dimension().location().toString());
        DataValueRegistry.registerBoolean(id("player.alive"), ctx -> ctx.minecraft().player != null && ctx.minecraft().player.isAlive());
        DataValueRegistry.registerBoolean(id("player.sneaking"), ctx -> ctx.minecraft().player != null && ctx.minecraft().player.isShiftKeyDown());
        DataValueRegistry.registerBoolean(id("player.creative"), ctx -> ctx.minecraft().player != null && ctx.minecraft().player.getAbilities().instabuild);
        DataValueRegistry.register(id("player.profile"), MinecraftDataSources::profile);
        DataValueRegistry.register(id("player.effects"), MinecraftDataSources::effects);
    }

    private static JsonObject profile(WidgetContext context) {
        JsonObject profile = new JsonObject();
        var player = context.minecraft().player;
        if (player == null) {
            profile.addProperty("name", "");
            profile.addProperty("health", 0);
            profile.addProperty("maxHealth", 20);
            profile.addProperty("level", 0);
            profile.addProperty("food", 0);
            profile.addProperty("alive", false);
            return profile;
        }
        profile.addProperty("name", player.getName().getString());
        profile.addProperty("health", player.getHealth());
        profile.addProperty("maxHealth", player.getMaxHealth());
        profile.addProperty("level", player.experienceLevel);
        profile.addProperty("food", player.getFoodData().getFoodLevel());
        profile.addProperty("alive", player.isAlive());
        return profile;
    }

    private static JsonArray effects(WidgetContext context) {
        JsonArray result = new JsonArray();
        var player = context.minecraft().player;
        if (player != null) {
            for (var effect : player.getActiveEffects()) {
                JsonObject entry = new JsonObject();
                entry.addProperty("name", effect.getEffect().value().getDisplayName().getString());
                entry.addProperty("amplifier", effect.getAmplifier() + 1);
                entry.addProperty("durationTicks", effect.getDuration());
                result.add(entry);
            }
        }
        return result;
    }

    private static void registerNumber(String path, NumericDataRegistry.Provider provider) {
        NumericDataRegistry.register(id(path), provider);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("anchorui", path);
    }
}
