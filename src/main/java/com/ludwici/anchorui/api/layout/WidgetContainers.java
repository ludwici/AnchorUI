package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.PositionElement;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.Map;
import java.util.Set;

public final class WidgetContainers {
    public static final String PARENT_FIELD = "__anchorui_container_parent";

    private WidgetContainers() {
    }

    public static <T> List<T> parentFirst(List<T> widgets, Function<T, String> id,
                                          Map<String, String> structuralParents) {
        Map<String, T> indexed = new LinkedHashMap<>();
        for (T widget : widgets) {
            indexed.put(id.apply(widget), widget);
        }
        List<T> result = new ArrayList<>(widgets.size());
        Set<String> seen = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String childId : indexed.keySet()) {
            appendParentFirst(childId, indexed, structuralParents, visiting, seen, result);
        }
        return List.copyOf(result);
    }

    private static <T> void appendParentFirst(String childId, Map<String, T> indexed,
                                              Map<String, String> structuralParents,
                                              Set<String> visiting, Set<String> seen, List<T> result) {
        if (seen.contains(childId)) {
            return;
        }
        if (!visiting.add(childId)) {
            throw new IllegalArgumentException("Circular structural parent: " + childId);
        }
        String parent = structuralParents.get(childId);
        if (parent != null && indexed.containsKey(parent)) {
            appendParentFirst(parent, indexed, structuralParents, visiting, seen, result);
        }
        visiting.remove(childId);
        seen.add(childId);
        result.add(indexed.get(childId));
    }

    public static boolean supportsChildren(String type) {
        return "anchorui:panel".equals(type) || "anchorui:container".equals(type)
                || "panel".equals(type) || "container".equals(type);
    }

    public static String contentId(String id) {
        return id + "/__content";
    }

    public static String parent(JsonObject settings) {
        if (!settings.has(PARENT_FIELD)) {
            return null;
        }
        return settings.get(PARENT_FIELD).getAsString();
    }

    public static AnchorLayout.Frame contentFrame(String id, int width, int height, PaddingElement padding) {
        int innerWidth = Math.max(0, width - padding.left - padding.right);
        int innerHeight = Math.max(0, height - padding.top - padding.bottom);
        PositionElement position = new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, id, padding.left, padding.top);
        return new AnchorLayout.Frame(contentId(id), position, innerWidth, innerHeight);
    }

    public static boolean clipChildren(JsonObject settings) {
        return settings.has("clipChildren") && settings.get("clipChildren").getAsBoolean();
    }

    public static WidgetTransforms.Group group(JsonObject settings) {
        double scale = settings.has("scale") ? settings.get("scale").getAsDouble() : 1.0;
        String origin = settings.has("scaleOrigin") ? settings.get("scaleOrigin").getAsString() : "top_left";
        if (!"top_left".equals(origin) && !"center".equals(origin)) {
            throw new IllegalArgumentException("scaleOrigin must be top_left or center");
        }
        return new WidgetTransforms.Group(scale, "center".equals(origin));
    }

    public static Map<String, AnchorLayout.Bounds> clips(Map<String, AnchorLayout.Bounds> bounds, Map<String, String> parents, Set<String> clippingContainers) {
        return clips(bounds, parents, clippingContainers, Map.of());
    }

    public static Map<String, AnchorLayout.Bounds> clips(Map<String, AnchorLayout.Bounds> bounds, Map<String, String> parents,
                                                         Set<String> clippingContainers, Map<String, WidgetTransforms.Transform> transforms) {
        Map<String, AnchorLayout.Bounds> result = new LinkedHashMap<>();
        AnchorLayout.Bounds screen = bounds.get("screen");
        if (screen == null) {
            return Map.of();
        }
        for (String id : parents.keySet()) {
            AnchorLayout.Bounds intersection = null;
            String parent = parents.get(id);
            int remaining = parents.size() + 1;
            while (parent != null && remaining-- > 0) {
                if (clippingContainers.contains(parent)) {
                    AnchorLayout.Bounds content = bounds.get(contentId(parent));
                    if (content == null) {
                        intersection = new AnchorLayout.Bounds(0, 0, 0, 0);
                        break;
                    }
                    WidgetTransforms.Transform transform = transforms.getOrDefault(parent, WidgetTransforms.IDENTITY);
                    AnchorLayout.Bounds visibleContent = transform.physical(content);
                    intersection = intersection == null ? intersect(screen, visibleContent) : intersect(intersection, visibleContent);
                }
                parent = parents.get(parent);
            }
            if (intersection != null) {
                result.put(id, intersection);
            }
        }
        return Map.copyOf(result);
    }

    private static AnchorLayout.Bounds intersect(AnchorLayout.Bounds first, AnchorLayout.Bounds second) {
        int left = Math.max(first.x(), second.x());
        int top = Math.max(first.y(), second.y());
        int right = Math.max(left, Math.min(first.x() + first.width(), second.x() + second.width()));
        int bottom = Math.max(top, Math.min(first.y() + first.height(), second.y() + second.height()));
        return new AnchorLayout.Bounds(left, top, right - left, bottom - top);
    }

    public static boolean contains(AnchorLayout.Bounds bounds, double x, double y) {
        return bounds != null && bounds.width() > 0 && bounds.height() > 0
                && x >= bounds.x() && x < bounds.x() + bounds.width()
                && y >= bounds.y() && y < bounds.y() + bounds.height();
    }
}
