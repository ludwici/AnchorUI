package com.ludwici.anchorui.api.style;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * One or more constraints on a widget's nine anchor points.
 * Two distinct horizontal/vertical points determine its width/height.
 */
public class PositionElement {
    public record Constraint(ElementAnchor point, ElementAnchor anchor, String relativeTo, int x, int y) {
        public Constraint {
            Objects.requireNonNull(point, "Anchor point must not be null");
            Objects.requireNonNull(anchor, "Target anchor must not be null");
            relativeTo = relativeTo == null || relativeTo.isBlank() ? "screen" : relativeTo;
        }

        public Constraint withTarget(String target) {
            return new Constraint(point, anchor, target, x, y);
        }

        public Constraint translated(int dx, int dy) {
            return new Constraint(point, anchor, relativeTo, Math.addExact(x, dx), Math.addExact(y, dy));
        }
    }

    public final ElementAnchor point;
    public final ElementAnchor anchor;
    public final String relativeTo;
    public final int x;
    public final int y;
    public final List<Constraint> anchors;

    public PositionElement(ElementAnchor point, ElementAnchor anchor, String relativeTo, int x, int y) {
        this(List.of(new Constraint(point, anchor, relativeTo, x, y)));
    }

    public PositionElement(List<Constraint> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            throw new IllegalArgumentException("Position requires at least one anchor");
        }
        this.anchors = List.copyOf(anchors);
        Constraint first = this.anchors.getFirst();
        point = first.point();
        anchor = first.anchor();
        relativeTo = first.relativeTo();
        x = first.x();
        y = first.y();
    }

    public PositionElement(JsonObject root) {
        this(parse(root));
    }

    public PositionElement withReferences(Function<String, String> resolver) {
        List<Constraint> updated = new ArrayList<>();
        for (Constraint entry : anchors) {
            updated.add(entry.withTarget(resolver.apply(entry.relativeTo())));
        }
        return new PositionElement(updated);
    }

    public PositionElement withPrimary(ElementAnchor point, ElementAnchor anchor, String relativeTo) {
        List<Constraint> updated = new ArrayList<>(anchors);
        Constraint first = updated.getFirst();
        updated.set(0, new Constraint(point, anchor, relativeTo, first.x(), first.y()));
        return new PositionElement(updated);
    }

    public PositionElement translated(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return this;
        }
        List<Constraint> updated = new ArrayList<>();
        for (Constraint entry : anchors) {
            updated.add(entry.translated(dx, dy));
        }
        return new PositionElement(updated);
    }

    public PositionElement paddedRelativeTo(String parent, PaddingElement padding) {
        List<Constraint> updated = new ArrayList<>();
        for (Constraint entry : anchors) {
            if (!parent.equals(entry.relativeTo())) {
                updated.add(entry);
                continue;
            }
            int dx = switch (entry.anchor().horizontal()) {
                case 0 -> padding.left;
                case 1 -> (padding.left - padding.right) / 2;
                default -> -padding.right;
            };
            int dy = switch (entry.anchor().vertical()) {
                case 0 -> padding.top;
                case 1 -> (padding.top - padding.bottom) / 2;
                default -> -padding.bottom;
            };
            updated.add(entry.translated(dx, dy));
        }
        return new PositionElement(updated);
    }

    public boolean dynamicWidth() {
        int first = point.horizontal();
        return anchors.stream().anyMatch(entry -> entry.point().horizontal() != first);
    }

    public boolean dynamicHeight() {
        int first = point.vertical();
        return anchors.stream().anyMatch(entry -> entry.point().vertical() != first);
    }

    private static List<Constraint> parse(JsonObject root) {
        if (root.has("anchors")) {
            JsonElement value = root.get("anchors");
            if (!value.isJsonArray()) {
                throw new IllegalArgumentException("position.anchors must be a non-empty array of anchor objects");
            }
            JsonArray array = value.getAsJsonArray();
            if (array.isEmpty()) {
                throw new IllegalArgumentException("position.anchors must contain at least one anchor");
            }
            if (root.has("point") || root.has("anchor") || root.has("relativeTo") || root.has("x") || root.has("y")) {
                throw new IllegalArgumentException("position.anchors cannot be combined with the single-anchor position fields");
            }
            List<Constraint> result = new ArrayList<>();
            for (int index = 0; index < array.size(); index++) {
                JsonElement entry = array.get(index);
                if (!entry.isJsonObject()) {
                    throw new IllegalArgumentException("position.anchors[" + index + "] must be an object");
                }
                try {
                    result.add(parseConstraint(entry.getAsJsonObject()));
                } catch (RuntimeException exception) {
                    throw new IllegalArgumentException("Invalid position.anchors[" + index + "]: " + exception.getMessage(), exception);
                }
            }
            return result;
        }
        return List.of(parseConstraint(root));
    }

    private static Constraint parseConstraint(JsonObject root) {
        ElementAnchor point = ElementAnchor.fromString(GsonHelper.getAsString(root, "point", GsonHelper.getAsString(root, "anchor", "TOPLEFT")));
        ElementAnchor anchor = ElementAnchor.fromString(GsonHelper.getAsString(root, "anchor", GsonHelper.getAsString(root, "point", "TOPLEFT")));
        return new Constraint(point, anchor, GsonHelper.getAsString(root, "relativeTo", "screen"), GsonHelper.getAsInt(root, "x", 0), GsonHelper.getAsInt(root, "y", 0));
    }
}
