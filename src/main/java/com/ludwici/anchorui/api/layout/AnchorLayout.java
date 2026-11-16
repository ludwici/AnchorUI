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

    /**
     * A screen-local snapshot of resolved anchor frames. Cache entries can be reused
     * only if the frame definition and its actual target bounds are unchanged.
     * The target chain is still visited, so broken references and cycles are
     * diagnosed on every resolution instead of being hidden by cached geometry.
     */
    public static final class Cache {
        private record Input(ElementAnchor point, ElementAnchor anchor, String relativeTo, int x, int y, int width, int height) {
            private static Input of(Frame frame) {
                PositionElement position = frame.position();
                return new Input(position.point, position.anchor, position.relativeTo, position.x, position.y, frame.width(), frame.height());
            }
        }

        private record Entry(Input input, Bounds target, Bounds result) {
        }

        private Map<String, Entry> entries = Map.of();

        public void clear() {
            entries = Map.of();
        }
    }

    private AnchorLayout() {
    }

    public static Map<String, Bounds> resolve(int screenWidth, int screenHeight, Collection<Frame> definitions) {
        return resolve(screenWidth, screenHeight, definitions, warning -> {});
    }

    public static Map<String, Bounds> resolve(int screenWidth, int screenHeight, Collection<Frame> definitions, Consumer<String> warningSink) {
        return resolve(screenWidth, screenHeight, definitions, null, warningSink);
    }

    public static Map<String, Bounds> resolve(int screenWidth, int screenHeight, Collection<Frame> definitions, Cache cache, Consumer<String> warningSink) {
        Map<String, Frame> frames = new LinkedHashMap<>();
        for (Frame frame : definitions) {
            if (frames.putIfAbsent(frame.id(), frame) != null) {
                throw new IllegalArgumentException("Duplicate frame id: " + frame.id());
            }
        }
        Map<String, Bounds> resolved = new LinkedHashMap<>();
        resolved.put("screen", new Bounds(0, 0, screenWidth, screenHeight));
        Map<String, Boolean> visiting = new HashMap<>();
        Map<String, Cache.Entry> next = cache == null ? null : new LinkedHashMap<>();
        for (String id : frames.keySet()) {
            place(id, frames, resolved, visiting, new ArrayList<>(), cache, next, warningSink);
        }
        if (cache != null) {
            cache.entries = Map.copyOf(next);
        }
        return Map.copyOf(resolved);
    }

    private static Bounds place(String id, Map<String, Frame> frames, Map<String, Bounds> resolved, Map<String, Boolean> visiting, List<String> path, Cache cache, Map<String, Cache.Entry> next, Consumer<String> warningSink) {
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
        if (targetId.endsWith(":__content") || targetId.endsWith("/__content")
                || targetId.contains("/__content/")) {
            throw new IllegalArgumentException("Unsupported widget anchor '" + targetId + "': reference the panel directly");
        }
        if (!resolved.containsKey(targetId) && !frames.containsKey(targetId)) {
            warningSink.accept("Frame '" + id + "' references unknown relativeTo '" + targetId + "'; falling back to screen");
            targetId = "screen";
        }
        Bounds target = place(targetId, frames, resolved, visiting, path, cache, next, warningSink);
        Cache.Input input = cache == null ? null : Cache.Input.of(frame);
        Cache.Entry previous = cache == null ? null : cache.entries.get(id);
        Bounds result;
        if (previous != null && previous.input().equals(input) && previous.target().equals(target)) {
            result = previous.result();
        } else {
            int x = target.x() + p.anchor.x(target.width()) + p.x - p.point.x(frame.width());
            int y = target.y() + p.anchor.y(target.height()) + p.y - p.point.y(frame.height());
            result = new Bounds(x, y, frame.width(), frame.height());
        }
        if (next != null) {
            next.put(id, new Cache.Entry(input, target, result));
        }
        resolved.put(id, result);
        path.remove(path.size() - 1);
        visiting.remove(id);
        return result;
    }
}
