package com.ludwici.anchorui.api.layout;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public final class WidgetTransforms {
    public static final Transform IDENTITY = new Transform(1.0, 0.0, 0.0);

    public record Group(double scale, boolean centerOrigin, boolean fitScreen, int fitMargin) {
        public Group(double scale, boolean centerOrigin) {
            this(scale, centerOrigin, false, 0);
        }

        public Group {
            if (!Double.isFinite(scale) || scale < 0.1 || scale > 4.0) {
                throw new IllegalArgumentException("Container scale must be between 0.1 and 4.0");
            }
            if (fitMargin < 0) {
                throw new IllegalArgumentException("Container fit margin must not be negative");
            }
        }

        public double effectiveScale(AnchorLayout.Bounds bounds, AnchorLayout.Bounds screen, double parentScale) {
            if (!fitScreen || screen == null || bounds.width() == 0 || bounds.height() == 0) {
                return scale;
            }
            double availableWidth = Math.max(1, screen.width() - 2.0 * fitMargin);
            double availableHeight = Math.max(1, screen.height() - 2.0 * fitMargin);
            double fit = Math.min(availableWidth / bounds.width(), availableHeight / bounds.height()) / parentScale;
            return Math.max(0.1, Math.min(4.0, scale * fit));
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
            return new Transform(scale * factor, translateX + scale * pivotX * (1 - factor), translateY + scale * pivotY * (1 - factor));
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
        return resolve(layout, parents, groups, Map.of());
    }

    public static Map<String, Transform> resolve(Map<String, AnchorLayout.Bounds> layout, Map<String, String> parents,
                                                  Map<String, Group> groups, Map<String, ContainerScroll.Offset> scrollOffsets) {
        Map<String, Transform> resolved = new LinkedHashMap<>();
        Set<String> visiting = new HashSet<>();
        for (String id : layout.keySet()) {
            if (!"screen".equals(id)) {
                resolveOne(id, layout, parents, groups, scrollOffsets, resolved, visiting);
            }
        }
        return Map.copyOf(resolved);
    }

    private static Transform resolveOne(String id, Map<String, AnchorLayout.Bounds> layout, Map<String, String> parents,
                                        Map<String, Group> groups, Map<String, ContainerScroll.Offset> scrollOffsets,
                                        Map<String, Transform> resolved, Set<String> visiting) {
        Transform existing = resolved.get(id);
        if (existing != null) {
            return existing;
        }
        if (!visiting.add(id)) {
            throw new IllegalArgumentException("Circular UI container hierarchy: " + id);
        }
        String parent = parents.get(id);
        Transform transform;
        if (parent == null) {
            transform = IDENTITY;
        } else {
            Transform parentTransform = resolveOne(parent, layout, parents, groups, scrollOffsets, resolved, visiting);
            ContainerScroll.Offset scroll = scrollOffsets.getOrDefault(parent, ContainerScroll.Offset.ZERO);
            transform = new Transform(parentTransform.scale(), parentTransform.translateX() - parentTransform.scale() * scroll.x(), parentTransform.translateY() - parentTransform.scale() * scroll.y());
        }
        AnchorLayout.Bounds bounds = layout.get(id);
        if (bounds == null) {
            throw new IllegalArgumentException("Missing layout bounds for UI widget: " + id);
        }
        Group group = groups.get(id);
        if (group != null) {
            double scale = group.effectiveScale(bounds, layout.get("screen"), transform.scale());
            double pivotX = bounds.x() + (group.centerOrigin() ? bounds.width() / 2.0 : 0.0);
            double pivotY = bounds.y() + (group.centerOrigin() ? bounds.height() / 2.0 : 0.0);
            transform = transform.scaledAround(pivotX, pivotY, scale);
        }
        resolved.put(id, transform);
        visiting.remove(id);
        return transform;
    }
}
