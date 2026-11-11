package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.PaddingElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ContainerLayout {
    public record AutoSize(boolean width, boolean height) {
        public static final AutoSize NONE = new AutoSize(false, false);

        public static AutoSize parse(JsonObject settings) {
            if (!settings.has("autoSize")) {
                return NONE;
            }
            JsonElement value = settings.get("autoSize");
            if (value.isJsonObject()) {
                JsonObject object = value.getAsJsonObject();
                return new AutoSize(object.has("width") && object.get("width").getAsBoolean(),
                        object.has("height") && object.get("height").getAsBoolean());
            }
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                return value.getAsBoolean() ? new AutoSize(true, true) : NONE;
            }
            return switch (value.getAsString()) {
                case "none" -> NONE;
                case "both" -> new AutoSize(true, true);
                case "width" -> new AutoSize(true, false);
                case "height" -> new AutoSize(false, true);
                default -> throw new IllegalArgumentException("autoSize must be none, width, height, both, boolean or an object");
            };
        }

        public boolean enabled() {
            return width || height;
        }
    }

    public record Size(int width, int height) {
    }

    public record Result(Map<String, AnchorLayout.Bounds> bounds, Map<String, Size> sizes) {
    }

    private ContainerLayout() {
    }

    public static Result resolve(int screenWidth, int screenHeight, Collection<AnchorLayout.Frame> frames,
                                 Map<String, PaddingElement> containers, Map<String, AutoSize> autoSizes,
                                 Map<String, String> parents, Consumer<String> warnings) {
        Map<String, AnchorLayout.Frame> definitions = new LinkedHashMap<>();
        for (AnchorLayout.Frame frame : frames) {
            if (definitions.putIfAbsent(frame.id(), frame) != null) {
                throw new IllegalArgumentException("Duplicate layout frame: " + frame.id());
            }
        }
        for (String id : containers.keySet()) {
            if (!definitions.containsKey(id)) {
                throw new IllegalArgumentException("Container is missing its widget frame: " + id);
            }
            if (definitions.containsKey(WidgetContainers.contentId(id))) {
                throw new IllegalArgumentException("Reserved container content frame ID: " + WidgetContainers.contentId(id));
            }
        }
        Map<String, List<String>> children = new LinkedHashMap<>();
        for (var entry : parents.entrySet()) {
            if (!containers.containsKey(entry.getValue())) {
                throw new IllegalArgumentException("Unknown structural container for '" + entry.getKey() + "': " + entry.getValue());
            }
            children.computeIfAbsent(entry.getValue(), ignored -> new ArrayList<>()).add(entry.getKey());
        }
        Map<String, Size> sizes = new LinkedHashMap<>();
        for (AnchorLayout.Frame frame : definitions.values()) {
            AutoSize auto = autoSizes.getOrDefault(frame.id(), AutoSize.NONE);
            sizes.put(frame.id(), new Size(auto.width() ? 1 : frame.width(), auto.height() ? 1 : frame.height()));
        }

        for (int iteration = 0; iteration < 32; iteration++) {
            List<AnchorLayout.Frame> all = new ArrayList<>(definitions.size() + containers.size());
            for (AnchorLayout.Frame frame : definitions.values()) {
                Size size = sizes.get(frame.id());
                all.add(new AnchorLayout.Frame(frame.id(), frame.position(), size.width(), size.height()));
                PaddingElement padding = containers.get(frame.id());
                if (padding != null) {
                    all.add(WidgetContainers.contentFrame(frame.id(), size.width(), size.height(), padding));
                }
            }
            Map<String, AnchorLayout.Bounds> resolved = AnchorLayout.resolve(screenWidth, screenHeight, all, warnings);
            boolean changed = false;
            for (var entry : autoSizes.entrySet()) {
                String id = entry.getKey();
                AutoSize auto = entry.getValue();
                if (!auto.enabled()) {
                    continue;
                }
                PaddingElement padding = containers.get(id);
                if (padding == null) {
                    throw new IllegalArgumentException("Only panel and container widgets support autoSize: " + id);
                }
                AnchorLayout.Bounds content = resolved.get(WidgetContainers.contentId(id));
                int contentWidth = 1;
                int contentHeight = 1;
                for (String childId : children.getOrDefault(id, List.of())) {
                    AnchorLayout.Bounds child = resolved.get(childId);
                    if (child != null) {
                        contentWidth = Math.max(contentWidth, child.x() + child.width() - content.x());
                        contentHeight = Math.max(contentHeight, child.y() + child.height() - content.y());
                    }
                }
                Size old = sizes.get(id);
                int nextWidth = auto.width() ? safeSize(contentWidth, padding.left, padding.right) : old.width();
                int nextHeight = auto.height() ? safeSize(contentHeight, padding.top, padding.bottom) : old.height();
                Size next = new Size(nextWidth, nextHeight);
                if (!next.equals(old)) {
                    sizes.put(id, next);
                    changed = true;
                }
            }
            if (!changed) {
                return new Result(resolved, Map.copyOf(sizes));
            }
        }
        throw new IllegalArgumentException("Container autoSize did not converge; avoid anchoring auto-sized children to the parent's right/center/bottom edge");
    }

    private static int safeSize(int content, int firstPadding, int lastPadding) {
        long result = Math.max(1, (long) content + firstPadding + lastPadding);
        if (result > 16384) {
            throw new IllegalArgumentException("Auto-sized container exceeds 16384 GUI pixels");
        }
        return (int) result;
    }
}
