package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.PaddingElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContainerScroll {
    public record Config(boolean horizontal, boolean vertical, int step) {
        public static final Config NONE = new Config(false, false, 18);

        public Config {
            if (step <= 0 || step > 1024) {
                throw new IllegalArgumentException("Container scroll step must be between 1 and 1024 GUI pixels");
            }
        }

        public boolean enabled() {
            return horizontal || vertical;
        }

        public static Config parse(JsonObject root) {
            if (!root.has("scroll")) {
                return NONE;
            }
            JsonElement value = root.get("scroll");
            if (value.isJsonPrimitive()) {
                String axis = value.getAsString();
                return switch (axis) {
                    case "none", "false" -> NONE;
                    case "vertical" -> new Config(false, true, 18);
                    case "horizontal" -> new Config(true, false, 18);
                    case "both" -> new Config(true, true, 18);
                    default -> throw new IllegalArgumentException("scroll must be none, vertical, horizontal, both or an object");
                };
            }
            JsonObject settings = value.getAsJsonObject();
            String axis = settings.has("axis") ? settings.get("axis").getAsString() : "vertical";
            int step = settings.has("step") ? settings.get("step").getAsInt() : 18;
            return switch (axis) {
                case "none" -> new Config(false, false, step);
                case "vertical" -> new Config(false, true, step);
                case "horizontal" -> new Config(true, false, step);
                case "both" -> new Config(true, true, step);
                default -> throw new IllegalArgumentException("Unknown scroll axis: " + axis);
            };
        }
    }

    public record Offset(int x, int y) {
        public static final Offset ZERO = new Offset(0, 0);
    }

    public record Limits(int x, int y) {
        public Offset clamp(Offset offset) {
            return new Offset(Math.max(0, Math.min(x, offset.x())),
                    Math.max(0, Math.min(y, offset.y())));
        }
    }

    private ContainerScroll() {
    }

    public static Map<String, Limits> measure(Map<String, AnchorLayout.Bounds> bounds,
                                              Map<String, String> parents, Map<String, Config> configurations,
                                              Map<String, PaddingElement> paddings) {
        Map<String, Limits> result = new LinkedHashMap<>();
        for (var entry : configurations.entrySet()) {
            String id = entry.getKey();
            Config config = entry.getValue();
            if (!config.enabled()) {
                continue;
            }
            AnchorLayout.Bounds content = WidgetContainers.contentBounds(bounds.get(id), paddings.get(id));
            if (content == null) {
                throw new IllegalArgumentException("Scrollable container has no content frame: " + id);
            }
            int maxRight = content.x();
            int maxBottom = content.y();
            for (var child : parents.entrySet()) {
                if (!id.equals(child.getValue())) {
                    continue;
                }
                AnchorLayout.Bounds box = bounds.get(child.getKey());
                if (box != null) {
                    maxRight = Math.max(maxRight, box.x() + box.width());
                    maxBottom = Math.max(maxBottom, box.y() + box.height());
                }
            }
            int overflowX = Math.max(0, maxRight - content.x() - content.width());
            int overflowY = Math.max(0, maxBottom - content.y() - content.height());
            result.put(id, new Limits(config.horizontal() ? overflowX : 0, config.vertical() ? overflowY : 0));
        }
        return Map.copyOf(result);
    }

    public static Offset move(Offset current, Limits limits, Config config, double scrollX, double scrollY) {
        int nextX = current.x() - (int) Math.round(scrollX * config.step());
        int nextY = current.y() - (int) Math.round(scrollY * config.step());
        if (config.vertical() && !config.horizontal() && scrollX != 0) {
            nextY -= (int) Math.round(scrollX * config.step());
        }
        if (config.horizontal() && !config.vertical() && scrollX == 0) {
            nextX -= (int) Math.round(scrollY * config.step());
        }
        return limits.clamp(new Offset(nextX, nextY));
    }
}
