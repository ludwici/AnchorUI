package com.ludwici.anchorui.api.layout;

import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class AnchorLayout {
    public record Bounds(int x, int y, int width, int height) {
        public Bounds {
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("Frame dimensions cannot be negative");
            }
        }
    }

    public record Frame(String id, PositionElement position, int width, int height) {
        public Frame {
            if (id == null || id.isBlank() || id.equals("screen")) {
                throw new IllegalArgumentException("Frame must have a unique id other than screen");
            }
            if (position == null || width < 0 || height < 0) {
                throw new IllegalArgumentException("Invalid frame definition: " + id);
            }
        }
    }

    private AnchorLayout() {
    }

    public static Map<String, Bounds> resolve(int screenWidth, int screenHeight, Collection<Frame> definitions) {
        return resolve(screenWidth, screenHeight, definitions, warning -> {});
    }

    public static Map<String, Bounds> resolve(int screenWidth, int screenHeight, Collection<Frame> definitions, Consumer<String> warningSink) {
        Map<String, Frame> frames = new LinkedHashMap<>();
        for (Frame frame : definitions) {
            if (frames.putIfAbsent(frame.id(), frame) != null) {
                throw new IllegalArgumentException("Duplicate frame id: " + frame.id());
            }
        }
        Map<String, Bounds> resolved = new LinkedHashMap<>();
        resolved.put("screen", new Bounds(0, 0, screenWidth, screenHeight));
        Map<String, Boolean> visiting = new HashMap<>();
        for (String id : frames.keySet()) {
            place(id, frames, resolved, visiting, new ArrayList<>(), warningSink);
        }
        return Map.copyOf(resolved);
    }

    private static Bounds place(String id, Map<String, Frame> frames, Map<String, Bounds> resolved, Map<String, Boolean> visiting, List<String> path, Consumer<String> warningSink) {
        Bounds existing = resolved.get(id);
        if (existing != null) {
            return existing;
        }
        Frame frame = frames.get(id);
        if (frame == null) {
            throw new IllegalArgumentException("Unknown relativeTo frame: " + id);
        }
        if (visiting.putIfAbsent(id, true) != null) {
            path.add(id);
            throw new IllegalArgumentException("Circular frame anchors: " + String.join(" -> ", path));
        }
        path.add(id);
        PositionElement p = frame.position();
        String targetId = p.relativeTo;
        if (!resolved.containsKey(targetId) && !frames.containsKey(targetId)) {
            warningSink.accept("Frame '" + id + "' references unknown relativeTo '" + targetId + "'; falling back to screen");
            targetId = "screen";
        }
        Bounds target = place(targetId, frames, resolved, visiting, path, warningSink);
        int x = target.x() + p.anchor.x(target.width()) + p.x - p.point.x(frame.width());
        int y = target.y() + p.anchor.y(target.height()) + p.y - p.point.y(frame.height());
        Bounds result = new Bounds(x, y, frame.width(), frame.height());
        resolved.put(id, result);
        path.remove(path.size() - 1);
        visiting.remove(id);
        return result;
    }
}
