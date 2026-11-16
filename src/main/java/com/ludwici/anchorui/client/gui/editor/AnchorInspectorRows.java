package com.ludwici.anchorui.client.gui.editor;

import com.ludwici.anchorui.api.style.PositionElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared read-only presentation of the effective anchors in both F8 inspectors.
 */
public final class AnchorInspectorRows {
    private AnchorInspectorRows() {
    }

    public static List<String> describe(List<PositionElement.Constraint> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (int index = 0; index < anchors.size(); index++) {
            PositionElement.Constraint anchor = anchors.get(index);
            result.add("#" + (index + 1) + (index == 0 ? " (primary)" : "") + ": " + anchor.point() + " -> " + anchor.anchor());
            result.add("  relativeTo: " + anchor.relativeTo());
            result.add("  offset: " + anchor.x() + ", " + anchor.y());
        }
        return List.copyOf(result);
    }

    public static String dimensions(List<PositionElement.Constraint> anchors) {
        if (anchors == null || anchors.isEmpty()) {
            return "";
        }
        PositionElement position = new PositionElement(anchors);
        return "size axes: " + (position.dynamicWidth() ? "width anchored" : "width fixed") + ", " + (position.dynamicHeight() ? "height anchored" : "height fixed");
    }
}
