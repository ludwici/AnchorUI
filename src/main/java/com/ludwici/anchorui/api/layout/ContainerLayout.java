package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.ElementAnchor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
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
        return resolve(screenWidth, screenHeight, frames, containers, autoSizes, parents,
                Map.of(), Set.of(), Set.of(), warnings);
    }

    public static Result resolve(int screenWidth, int screenHeight, Collection<AnchorLayout.Frame> frames,
                                 Map<String, PaddingElement> containers, Map<String, AutoSize> autoSizes,
                                 Map<String, String> parents, Map<String, ? extends ContainerAutoLayout> layouts,
                                 Set<String> hidden, Set<String> unmanaged, Consumer<String> warnings) {
        return resolve(screenWidth, screenHeight, frames, containers, autoSizes, parents,
                layouts, Map.of(), hidden, unmanaged, warnings);
    }

    public static Result resolve(int screenWidth, int screenHeight, Collection<AnchorLayout.Frame> frames,
                                 Map<String, PaddingElement> containers, Map<String, AutoSize> autoSizes,
                                 Map<String, String> parents, Map<String, ? extends ContainerAutoLayout> layouts,
                                 Map<String, StackLayout.Rules> itemRules,
                                 Set<String> hidden, Set<String> unmanaged, Consumer<String> warnings) {
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
        for (String id : layouts.keySet()) {
            if (!containers.containsKey(id)) {
                throw new IllegalArgumentException("Automatic layout requires a panel or container: " + id);
            }
        }
        Map<String, List<String>> children = new LinkedHashMap<>();
        for (var entry : parents.entrySet()) {
            if (!containers.containsKey(entry.getValue())) {
                throw new IllegalArgumentException("Unknown structural container for '" + entry.getKey() + "': " + entry.getValue());
            }
            structuralDepth(entry.getKey(), parents);
        }
        for (String childId : definitions.keySet()) {
            String parent = parents.get(childId);
            if (parent != null) {
                children.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(childId);
            }
        }
        List<String> layoutOrder = new ArrayList<>(layouts.keySet());
        layoutOrder.sort(java.util.Comparator.comparingInt(id -> structuralDepth(id, parents)));
        Map<String, Size> sizes = new LinkedHashMap<>();
        for (AnchorLayout.Frame frame : definitions.values()) {
            AutoSize auto = autoSizes.getOrDefault(frame.id(), AutoSize.NONE);
            sizes.put(frame.id(), new Size(auto.width() ? 1 : frame.width(), auto.height() ? 1 : frame.height()));
        }

        for (int iteration = 0; iteration < 32; iteration++) {
            Map<String, AnchorLayout.Frame> effective = new LinkedHashMap<>();
            for (AnchorLayout.Frame frame : definitions.values()) {
                Size size = sizes.get(frame.id());
                effective.put(frame.id(), new AnchorLayout.Frame(frame.id(), frame.position(), size.width(), size.height()));
            }
            Set<String> managed = new HashSet<>();
            Map<String, Size> intrinsic = new LinkedHashMap<>();
            for (String container : layoutOrder) {
                ContainerAutoLayout layout = layouts.get(container);
                PaddingElement padding = containers.get(container);
                AnchorLayout.Frame outer = effective.get(container);
                int availableWidth = Math.max(0, outer.width() - padding.left - padding.right);
                int availableHeight = Math.max(0, outer.height() - padding.top - padding.bottom);
                List<StackLayout.Item> items = new ArrayList<>();
                boolean grid = layout instanceof GridLayout.Config;
                for (String childId : children.getOrDefault(container, List.of())) {
                    AnchorLayout.Frame child = effective.get(childId);
                    if (child == null || (layout.collapseHidden() && hidden.contains(childId))) {
                        continue;
                    }
                    var position = child.position();
                    StackLayout.Rules rules = itemRules.getOrDefault(childId, StackLayout.Rules.DEFAULT);
                    if (!grid) {
                        if (unmanaged.contains(childId) || !rules.managed()
                                || !WidgetContainers.contentId(container).equals(position.relativeTo)
                                || position.point != ElementAnchor.TOPLEFT
                                || position.anchor != ElementAnchor.TOPLEFT) {
                            continue;
                        }
                    }
                    int offsetX = grid ? 0 : position.x;
                    int offsetY = grid ? 0 : position.y;
                    items.add(new StackLayout.Item(childId, child.width(), child.height(), offsetX, offsetY, rules));
                }
                AutoSize auto = autoSizes.getOrDefault(container, AutoSize.NONE);
                ContainerAutoLayout.Arrangement arrangement =
                        layout.arrange(availableWidth, availableHeight, items, auto.width(), auto.height());
                intrinsic.put(container, arrangement.intrinsic());
                for (var positioned : arrangement.placements().entrySet()) {
                    StackLayout.Placement p = positioned.getValue();
                    String childId = positioned.getKey();
                    var position = new com.ludwici.anchorui.api.style.PositionElement(
                            ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT,
                            WidgetContainers.contentId(container), p.x(), p.y());
                    effective.put(childId, new AnchorLayout.Frame(childId, position, p.width(), p.height()));
                    managed.add(childId);
                }
            }

            List<AnchorLayout.Frame> all = new ArrayList<>(effective.size() + containers.size());
            for (AnchorLayout.Frame frame : effective.values()) {
                all.add(frame);
                PaddingElement padding = containers.get(frame.id());
                if (padding != null) {
                    all.add(WidgetContainers.contentFrame(frame.id(), frame.width(), frame.height(), padding));
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
                    ContainerAutoLayout automatic = layouts.get(id);
                    if (managed.contains(childId)
                            || (automatic != null && automatic.collapseHidden() && hidden.contains(childId))) {
                        continue;
                    }
                    AnchorLayout.Bounds child = resolved.get(childId);
                    if (child != null) {
                        contentWidth = Math.max(contentWidth, child.x() + child.width() - content.x());
                        contentHeight = Math.max(contentHeight, child.y() + child.height() - content.y());
                    }
                }
                Size measured = intrinsic.get(id);
                if (measured != null) {
                    contentWidth = Math.max(contentWidth, measured.width());
                    contentHeight = Math.max(contentHeight, measured.height());
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
                Map<String, Size> actualSizes = new LinkedHashMap<>();
                for (AnchorLayout.Frame frame : effective.values()) {
                    actualSizes.put(frame.id(), new Size(frame.width(), frame.height()));
                }
                return new Result(resolved, Map.copyOf(actualSizes));
            }
        }
        throw new IllegalArgumentException("Container autoSize did not converge; avoid anchoring auto-sized children to the parent's right/center/bottom edge");
    }

    private static int structuralDepth(String id, Map<String, String> parents) {
        Set<String> visited = new HashSet<>();
        int depth = 0;
        String current = id;
        while ((current = parents.get(current)) != null) {
            if (!visited.add(current)) {
                throw new IllegalArgumentException("Circular structural container parent near '" + id + "'");
            }
            depth++;
            if (depth > 256) {
                throw new IllegalArgumentException("Container nesting exceeds 256 at '" + id + "'");
            }
        }
        return depth;
    }

    private static int safeSize(int content, int firstPadding, int lastPadding) {
        long result = Math.max(1, (long) content + firstPadding + lastPadding);
        if (result > 16384) {
            throw new IllegalArgumentException("Auto-sized container exceeds 16384 GUI pixels");
        }
        return (int) result;
    }
}
