package com.ludwici.anchorui.client.hud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HudSnap {
    public static final int THRESHOLD = 6;

    public record Widget(String id, String relativeTo, int x, int y, int width, int height, boolean visible) {
    }

    public record Result(int x, int y, Integer guideX, Integer guideY) {
    }

    private record Candidate(int correction, int guide) {
    }

    private HudSnap() {
    }

    public static Result snap(Widget moving, int desiredX, int desiredY, List<Widget> widgets) {
        Map<String, Widget> byId = new HashMap<>();
        for (Widget widget : widgets) {
            byId.put(widget.id(), widget);
        }

        Candidate horizontal = null;
        Candidate vertical = null;
        for (Widget other : widgets) {
            if (!other.visible() || other.width() <= 0 || other.height() <= 0 || other.id().equals(moving.id()) || follows(other, moving.id(), byId)) {
                continue;
            }

            horizontal = closest(horizontal, desiredX, other.x());
            horizontal = closest(horizontal, desiredX + moving.width() / 2, other.x() + other.width() / 2);
            horizontal = closest(horizontal, desiredX + moving.width(), other.x() + other.width());
            horizontal = closest(horizontal, desiredX, other.x() + other.width());
            horizontal = closest(horizontal, desiredX + moving.width(), other.x());

            vertical = closest(vertical, desiredY, other.y());
            vertical = closest(vertical, desiredY + moving.height() / 2, other.y() + other.height() / 2);
            vertical = closest(vertical, desiredY + moving.height(), other.y() + other.height());
            vertical = closest(vertical, desiredY, other.y() + other.height());
            vertical = closest(vertical, desiredY + moving.height(), other.y());
        }
        return new Result(
                desiredX + (horizontal == null ? 0 : horizontal.correction()),
                desiredY + (vertical == null ? 0 : vertical.correction()),
                horizontal == null ? null : horizontal.guide(),
                vertical == null ? null : vertical.guide()
        );
    }

    private static Candidate closest(Candidate current, int movingCoordinate, int referenceCoordinate) {
        int correction = referenceCoordinate - movingCoordinate;
        if (Math.abs(correction) > THRESHOLD) {
            return current;
        }
        if (current == null || Math.abs(correction) < Math.abs(current.correction())) {
            return new Candidate(correction, referenceCoordinate);
        }
        return current;
    }

    private static boolean follows(Widget widget, String movingId, Map<String, Widget> byId) {
        String parent = widget.relativeTo();
        if (parent.endsWith("/__content")) {
            parent = parent.substring(0, parent.length() - "/__content".length());
        }
        for (int i = 0; i < byId.size(); i++) {
            if (movingId.equals(parent)) {
                return true;
            }
            Widget ancestor = byId.get(parent);
            if (ancestor == null) {
                return false;
            }
            parent = ancestor.relativeTo();
        }
        return false;
    }
}
