package com.ludwici.anchorui.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.scores.DisplaySlot;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class VanillaHudAnchors {
    private static final long HOTBAR = bit("minecraft:hotbar");
    private static final long CHAT = bit("minecraft:chat");
    private static final long CROSSHAIR = bit("minecraft:crosshair");
    private static final long SPECTATOR_TOOLTIP = bit("minecraft:spectator_tooltip");
    private static final long PLAYER_HEALTH = bit("minecraft:player_health");
    private static final long EXPERIENCE_BAR = bit("minecraft:experience_bar");
    private static final long FOOD_LEVEL = bit("minecraft:food_level");
    private static final long EXPERIENCE_LEVEL = bit("minecraft:experience_level");
    private static final long ARMOR_LEVEL = bit("minecraft:armor_level");
    private static final long AIR_LEVEL = bit("minecraft:air_level");
    private static final long VEHICLE_HEALTH = bit("minecraft:vehicle_health");
    private static final long JUMP_METER = bit("minecraft:jump_meter");
    private static final long EFFECTS = bit("minecraft:effects");
    private static final long SLEEP_OVERLAY = bit("minecraft:sleep_overlay");
    private static final long CAMERA_OVERLAYS = bit("minecraft:camera_overlays");
    private static final long SCOREBOARD_SIDEBAR = bit("minecraft:scoreboard_sidebar");

    public record Snapshot(long activeMask, int heartRows, int effectRows) {
        public Snapshot {
            heartRows = Math.max(1, Math.min(10, heartRows));
            effectRows = Math.max(0, effectRows);
        }
    }

    private VanillaHudAnchors() {
    }

    private static long bit(String id) {
        int index = VanillaHudGeometry.IDS.indexOf(id);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown vanilla HUD layer: " + id);
        }
        return 1L << index;
    }

    public static Snapshot snapshot(Minecraft minecraft) {
        var player = minecraft.player;
        if (player == null) {
            return new Snapshot(0, 1, 0);
        }

        boolean spectator = player.isSpectator();
        boolean survival = !spectator && !player.getAbilities().instabuild;
        boolean vehicle = player.getVehicle() instanceof LivingEntity;
        boolean jumping = player.getVehicle() instanceof PlayerRideableJumping;
        boolean underwater = player.isUnderWater() || player.getAirSupply() < player.getMaxAirSupply();
        int effectCount = player.getActiveEffects().size();
        int heartRows = (int) Math.ceil((player.getMaxHealth() + player.getAbsorptionAmount()) / 20.0);

        long active = HOTBAR | CHAT;
        active |= spectator ? SPECTATOR_TOOLTIP : CROSSHAIR;
        if (survival) {
            active |= PLAYER_HEALTH | EXPERIENCE_BAR | FOOD_LEVEL;
            if (player.experienceLevel > 0) {
                active |= EXPERIENCE_LEVEL;
            }
            if (player.getArmorValue() > 0) {
                active |= ARMOR_LEVEL;
            }
            if (underwater) {
                active |= AIR_LEVEL;
            }
        }
        if (vehicle) {
            active |= VEHICLE_HEALTH;
            active &= ~FOOD_LEVEL;
        }
        if (jumping) {
            active |= JUMP_METER;
            active &= ~EXPERIENCE_BAR;
        }
        if (effectCount > 0) {
            active |= EFFECTS;
        }
        if (player.isSleeping()) {
            active |= SLEEP_OVERLAY;
        }
        if (player.isOnFire()) {
            active |= CAMERA_OVERLAYS;
        }
        if (minecraft.level != null && minecraft.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR) != null) {
            active |= SCOREBOARD_SIDEBAR;
        }
        return new Snapshot(active, heartRows, (effectCount + 4) / 5);
    }

    public static Map<String, VanillaHudGeometry.Reference> measure(Snapshot snapshot, int width, int height) {
        Set<String> active = new HashSet<>();
        for (int index = 0; index < VanillaHudGeometry.IDS.size(); index++) {
            if ((snapshot.activeMask() & (1L << index)) != 0) {
                active.add(VanillaHudGeometry.IDS.get(index));
            }
        }
        return VanillaHudGeometry.measure(width, height, new VanillaHudGeometry.State(active, snapshot.heartRows(), snapshot.effectRows() * 5));
    }

    public static Map<String, VanillaHudGeometry.Reference> capture(Minecraft minecraft, int width, int height) {
        return measure(snapshot(minecraft), width, height);
    }
}
