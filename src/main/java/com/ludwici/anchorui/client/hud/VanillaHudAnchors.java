package com.ludwici.anchorui.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.scores.DisplaySlot;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class VanillaHudAnchors {
    private VanillaHudAnchors() {
    }

    public static Map<String, VanillaHudGeometry.Reference> capture(Minecraft minecraft, int width, int height) {
        Set<String> active = new HashSet<>();
        var player = minecraft.player;
        boolean spectator = player != null && player.isSpectator();
        boolean survival = player != null && !spectator && !player.getAbilities().instabuild;
        boolean vehicle = player != null && player.getVehicle() instanceof LivingEntity;
        boolean jumping = player != null && player.getVehicle() instanceof PlayerRideableJumping;
        boolean underwater = player != null && (player.isUnderWater() || player.getAirSupply() < player.getMaxAirSupply());
        int effects = player == null ? 0 : player.getActiveEffects().size();
        int heartRows = player == null ? 1 : (int) Math.ceil(
                (player.getMaxHealth() + player.getAbsorptionAmount()) / 20.0);

        if (player != null) {
            active.add("minecraft:hotbar");
            active.add("minecraft:chat");
            if (!spectator) {
                active.add("minecraft:crosshair");
            } else {
                active.add("minecraft:spectator_tooltip");
            }
            if (survival) {
                active.add("minecraft:player_health");
                active.add("minecraft:experience_bar");
                active.add("minecraft:food_level");
                if (player.experienceLevel > 0) {
                    active.add("minecraft:experience_level");
                }
                if (player.getArmorValue() > 0) {
                    active.add("minecraft:armor_level");
                }
                if (underwater) {
                    active.add("minecraft:air_level");
                }
            }
            if (vehicle) {
                active.add("minecraft:vehicle_health");
                active.remove("minecraft:food_level");
            }
            if (jumping) {
                active.add("minecraft:jump_meter");
                active.remove("minecraft:experience_bar");
            }
            if (effects > 0) {
                active.add("minecraft:effects");
            }
            if (player.isSleeping()) {
                active.add("minecraft:sleep_overlay");
            }
            if (player.isOnFire()) {
                active.add("minecraft:camera_overlays");
            }
            if (minecraft.level != null && minecraft.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR) != null) {
                active.add("minecraft:scoreboard_sidebar");
            }
        }

        return VanillaHudGeometry.measure(width, height, new VanillaHudGeometry.State(active, heartRows, effects));
    }
}
