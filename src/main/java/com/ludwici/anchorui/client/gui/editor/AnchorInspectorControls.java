package com.ludwici.anchorui.client.gui.editor;

import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;

import java.util.ArrayList;
import java.util.List;

/** Immutable edits to the complete set of widget anchor constraints. */
public final class AnchorInspectorControls {
    private AnchorInspectorControls() {
    }

    public static List<PositionElement.Constraint> add(List<PositionElement.Constraint> entries) {
        List<PositionElement.Constraint> result = new ArrayList<>(entries);
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Anchor list cannot be empty");
        }
        result.add(result.getLast());
        return List.copyOf(result);
    }

    public static List<PositionElement.Constraint> remove(List<PositionElement.Constraint> entries, int index) {
        checkIndex(entries, index);
        if (entries.size() == 1) {
            throw new IllegalArgumentException("Widget must retain at least one anchor");
        }
        List<PositionElement.Constraint> result = new ArrayList<>(entries);
        result.remove(index);
        return List.copyOf(result);
    }

    public static List<PositionElement.Constraint> change(List<PositionElement.Constraint> entries, int index, String field, String value) {
        checkIndex(entries, index);
        PositionElement.Constraint old = entries.get(index);
        PositionElement.Constraint edited = switch (field) {
            case "point" -> new PositionElement.Constraint(ElementAnchor.fromString(value), old.anchor(), old.relativeTo(), old.x(), old.y());
            case "anchor" -> new PositionElement.Constraint(old.point(), ElementAnchor.fromString(value), old.relativeTo(), old.x(), old.y());
            case "relativeTo" -> new PositionElement.Constraint(old.point(), old.anchor(), value, old.x(), old.y());
            case "x" -> new PositionElement.Constraint(old.point(), old.anchor(), old.relativeTo(), Integer.parseInt(value.trim()), old.y());
            case "y" -> new PositionElement.Constraint(old.point(), old.anchor(), old.relativeTo(), old.x(), Integer.parseInt(value.trim()));
            default -> throw new IllegalArgumentException("Unknown anchor field: " + field);
        };
        List<PositionElement.Constraint> result = new ArrayList<>(entries);
        result.set(index, edited);
        return List.copyOf(result);
    }

    public static List<PositionElement.Constraint> step(List<PositionElement.Constraint> entries, int index, String field, int change) {
        checkIndex(entries, index);
        PositionElement.Constraint old = entries.get(index);
        int next = switch (field) {
            case "x" -> Math.addExact(old.x(), change);
            case "y" -> Math.addExact(old.y(), change);
            default -> throw new IllegalArgumentException("Not a numeric anchor field: " + field);
        };
        return change(entries, index, field, Integer.toString(next));
    }

    private static void checkIndex(List<PositionElement.Constraint> entries, int index) {
        if (entries == null || index < 0 || index >= entries.size()) {
            throw new IllegalArgumentException("Unknown anchor index: " + index);
        }
    }
}
