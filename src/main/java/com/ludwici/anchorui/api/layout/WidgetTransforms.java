package com.ludwici.anchorui.api.layout;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public final class WidgetTransforms {
    public static final Transform IDENTITY = new Transform(1.0, 0.0, 0.0);

    public record Group(double scale, boolean centerOrigin) {
        public Group {
            if (!Double.isFinite(scale) || scale < 0.1 || scale > 4.0) {
                throw new IllegalArgumentException("Container scale must be between 0.1 and 4.0");
            }
        }
    }

    public record Transform(double scale, double translateX, double translateY) {
        public double x(double logicalX) {
            return scale * logicalX + translateX;
        }

        public double y(double logicalY) {
            return scale * logicalY + translateY;
        }

        public double inverseX(double physicalX) {
            return (physicalX - translateX) / scale;
        }

        public double inverseY(double physicalY) {
            return (physicalY - translateY) / scale;
        }

        public Transform scaledAround(double pivotX, double pivotY, double factor) {
            return new Transform(scale * factor,
                    translateX + scale * pivotX * (1 - factor),
                    translateY + scale * pivotY * (1 - factor));
        }

        public AnchorLayout.Bounds physical(AnchorLayout.Bounds logical) {
            int left = (int) Math.floor(x(logical.x()));
            int top = (int) Math.floor(y(logical.y()));
            int right = (int) Math.ceil(x(logical.x() + logical.width()));
            int bottom = (int) Math.ceil(y(logical.y() + logical.height()));
            return new AnchorLayout.Bounds(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
        }
    }

    private WidgetTransforms() {
    }

    public static Map<String, Transform> resolve(Map<String, AnchorLayout.Bounds> layout, Map<String, String> parents, Map<String, Group> groups) {
        Map<String, Transform> resolved = new LinkedHashMap<>();
        Set<String> visiting = new HashSet<>();
        for (String id : layout.keySet()) {
            if (!"screen".equals(id) && !id.endsWith("/__content")) {
                resolveOne(id, layout, parents, groups, resolved, visiting);
            }
        }
        return Map.copyOf(resolved);
    }

    private static Transform resolveOne(String id, Map<String, AnchorLayout.Bounds> layout, Map<String, String> parents,
                                        Map<String, Group> groups, Map<String, Transform> resolved, Set<String> visiting) {
        Transform existing = resolved.get(id);
        if (existing != null) {
            return existing;
        }
        if (!visiting.add(id)) {
            throw new IllegalArgumentException("Circular UI container hierarchy: " + id);
        }
        String parent = parents.get(id);
        Transform transform = parent == null ? IDENTITY : resolveOne(parent, layout, parents, groups, resolved, visiting);
        AnchorLayout.Bounds bounds = layout.get(id);
        if (bounds == null) {
            throw new IllegalArgumentException("Missing layout bounds for UI widget: " + id);
        }
        Group group = groups.get(id);
        if (group != null && group.scale() != 1.0) {
            double pivotX = bounds.x() + (group.centerOrigin() ? bounds.width() / 2.0 : 0.0);
            double pivotY = bounds.y() + (group.centerOrigin() ? bounds.height() / 2.0 : 0.0);
            transform = transform.scaledAround(pivotX, pivotY, group.scale());
        }
        resolved.put(id, transform);
        visiting.remove(id);
        return transform;
    }
}
