package com.ludwici.anchorui.client.hud;

import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VanillaHudGeometry {
    public static final List<String> IDS = List.of(
            "minecraft:camera_overlays",
            "minecraft:crosshair",
            "minecraft:hotbar",
            "minecraft:jump_meter",
            "minecraft:experience_bar",
            "minecraft:player_health",
            "minecraft:armor_level",
            "minecraft:food_level",
            "minecraft:vehicle_health",
            "minecraft:air_level",
            "minecraft:selected_item_name",
            "minecraft:spectator_tooltip",
            "minecraft:experience_level",
            "minecraft:effects",
            "minecraft:boss_overlay",
            "minecraft:sleep_overlay",
            "minecraft:demo_overlay",
            "minecraft:debug_overlay",
            "minecraft:scoreboard_sidebar",
            "minecraft:overlay_message",
            "minecraft:title",
            "minecraft:chat",
            "minecraft:tab_list",
            "minecraft:subtitle_overlay",
            "minecraft:saving_indicator"
    );

    public record Reference(String id, AnchorLayout.Bounds bounds, boolean active) {
    }

    public record State(Set<String> activeIds, int heartRows, int effectCount) {
        public State {
            activeIds = Set.copyOf(activeIds);
            heartRows = Math.max(1, Math.min(10, heartRows));
            effectCount = Math.max(0, effectCount);
        }

        public boolean active(String id) {
            return activeIds.contains(id);
        }
    }

    private VanillaHudGeometry() {
    }

    public static boolean reserved(String id) {
        return IDS.contains(id);
    }

    public static Map<String, Reference> measure(int width, int height, State state) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Invalid HUD dimensions");
        }
        int center = width / 2;
        int base = height - 39;
        int left = center - 91;
        int right = center + 10;
        int heartRows = state.heartRows();
        int heartHeight = 9 + (heartRows - 1) * 10;
        int effectRows = (state.effectCount() + 4) / 5;

        Map<String, Reference> references = new LinkedHashMap<>();
        put(references, state, "minecraft:camera_overlays", 0, 0, width, height);
        put(references, state, "minecraft:crosshair", center - 8, height / 2 - 8, 16, 16);
        put(references, state, "minecraft:hotbar", center - 91, height - 22, 182, 22);
        put(references, state, "minecraft:jump_meter", center - 91, height - 29, 182, 5);
        put(references, state, "minecraft:experience_bar", center - 91, height - 29, 182, 5);
        put(references, state, "minecraft:player_health", left, base - (heartRows - 1) * 10, 81, heartHeight);
        put(references, state, "minecraft:armor_level", left, base - heartRows * 10, 81, 9);
        put(references, state, "minecraft:food_level", right, base, 81, 9);
        put(references, state, "minecraft:vehicle_health", right, base, 81, 9);
        put(references, state, "minecraft:air_level", right, base - 10, 81, 9);
        put(references, state, "minecraft:selected_item_name", center - 100, height - 64, 200, 13);
        put(references, state, "minecraft:spectator_tooltip", center - 100, height - 52, 200, 18);
        put(references, state, "minecraft:experience_level", center - 24, height - 40, 48, 14);
        put(references, state, "minecraft:effects", width - 133, 2, 130, Math.max(25, effectRows * 26));
        put(references, state, "minecraft:boss_overlay", center - 91, 12, 182, Math.min(Math.max(12, height / 3), 130));
        put(references, state, "minecraft:sleep_overlay", 0, 0, width, height);
        put(references, state, "minecraft:demo_overlay", width - 225, 5, 220, 20);
        put(references, state, "minecraft:debug_overlay", 0, 0, width, height);
        put(references, state, "minecraft:scoreboard_sidebar", width - 164, height / 3, 160, Math.min(160, height / 2));
        put(references, state, "minecraft:overlay_message", center - 130, height - 79, 260, 24);
        put(references, state, "minecraft:title", center - 160, height / 2 - 45, 320, 90);
        put(references, state, "minecraft:chat", 2, height - 177, Math.min(320, width - 4), Math.min(160, height - 5));
        put(references, state, "minecraft:tab_list", center - Math.min(200, width / 2), 4, Math.min(400, width), Math.min(280, height - 8));
        put(references, state, "minecraft:subtitle_overlay", width - 170, height - 100, 166, 90);
        put(references, state, "minecraft:saving_indicator", width - 165, height - 46, 160, 30);
        return Map.copyOf(references);
    }

    private static void put(Map<String, Reference> references, State state, String id,
                            int x, int y, int width, int height) {
        references.put(id, new Reference(id, new AnchorLayout.Bounds(x, y, Math.max(0, width), Math.max(0, height)), state.active(id)));
    }

    public static List<AnchorLayout.Frame> frames(Map<String, Reference> references) {
        List<AnchorLayout.Frame> frames = new ArrayList<>();
        for (String id : IDS) {
            Reference ref = references.get(id);
            if (ref == null) {
                throw new IllegalArgumentException("Missing vanilla HUD reference: " + id);
            }
            AnchorLayout.Bounds bounds = ref.bounds();
            PositionElement position = new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT,
                    "screen", bounds.x(), bounds.y());
            frames.add(new AnchorLayout.Frame(id, position, bounds.width(), bounds.height()));
        }
        return List.copyOf(frames);
    }
}
