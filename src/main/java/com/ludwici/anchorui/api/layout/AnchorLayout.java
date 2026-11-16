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
     * Dependency-aware cache. Every constraint target is visited even on cache
     * hits to preserve warnings, detect cycles and invalidate stretched sizes.
     */
    public static final class Cache {
        private record Input(List<PositionElement.Constraint> anchors, int width, int height) {
            private static Input of(Frame frame) {
                return new Input(frame.position().anchors, frame.width(), frame.height());
            }
        }

        private record Entry(Input input, List<Bounds> targets, Bounds result) {
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
        List<Bounds> targets = new ArrayList<>();
        int index = 0;
        for (PositionElement.Constraint constraint : frame.position().anchors) {
            String targetId = constraint.relativeTo();
            if (targetId.endsWith(":__content") || targetId.endsWith("/__content") || targetId.contains("/__content/")) {
                throw new IllegalArgumentException("Unsupported widget anchor '" + targetId + "': reference the panel directly");
            }
            if (!resolved.containsKey(targetId) && !frames.containsKey(targetId)) {
                warningSink.accept("Frame '" + id + "' anchor #" + index + " references unknown relativeTo '" + targetId + "'; falling back to screen");
                targetId = "screen";
            }
            targets.add(place(targetId, frames, resolved, visiting, path, cache, next, warningSink));
            index++;
        }

        Cache.Input input = cache == null ? null : Cache.Input.of(frame);
        Cache.Entry previous = cache == null ? null : cache.entries.get(id);
        Bounds result;
        if (previous != null && previous.input().equals(input) && previous.targets().equals(targets)) {
            result = previous.result();
        } else {
            Axis horizontal = solve(frame.id(), frame.position().anchors, targets, frame.width(), true);
            Axis vertical = solve(frame.id(), frame.position().anchors, targets, frame.height(), false);
            result = new Bounds(horizontal.start(), vertical.start(), horizontal.size(), vertical.size());
        }
        if (next != null) {
            next.put(id, new Cache.Entry(input, List.copyOf(targets), result));
        }
        resolved.put(id, result);
        path.remove(path.size() - 1);
        visiting.remove(id);
        return result;
    }

    private record Axis(int start, int size) {
    }

    private static Axis solve(String id, List<PositionElement.Constraint> constraints, List<Bounds> targets, int preferredSize, boolean horizontal) {
        Long[] coordinates = new Long[3];
        String axis = horizontal ? "horizontal" : "vertical";
        for (int index = 0; index < constraints.size(); index++) {
            PositionElement.Constraint constraint = constraints.get(index);
            Bounds target = targets.get(index);
            int factor = horizontal ? constraint.point().horizontal() : constraint.point().vertical();
            ElementAnchor targetPoint = constraint.anchor();
            int targetFactor = horizontal ? targetPoint.horizontal() : targetPoint.vertical();
            long origin = horizontal ? target.x() : target.y();
            long size = horizontal ? target.width() : target.height();
            long offset = horizontal ? constraint.x() : constraint.y();
            long coordinate = origin + size * targetFactor / 2 + offset;
            if (coordinates[factor] != null && coordinates[factor] != coordinate) {
                throw new IllegalArgumentException("Conflicting " + axis + " anchors for frame '" + id + "': point " + constraint.point() + " resolves to " + coordinate + " instead of " + coordinates[factor]);
            }
            coordinates[factor] = coordinate;
        }

        long length = preferredSize;
        if (coordinates[0] != null && coordinates[2] != null) {
            length = coordinates[2] - coordinates[0];
        } else if (coordinates[0] != null && coordinates[1] != null) {
            length = 2 * (coordinates[1] - coordinates[0]);
        } else if (coordinates[1] != null && coordinates[2] != null) {
            length = 2 * (coordinates[2] - coordinates[1]);
        }
        if (length < 0 || length > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid " + axis + " span for frame '" + id + "': computed size " + length);
        }
        int first = coordinates[0] != null ? 0 : coordinates[1] != null ? 1 : 2;
        long origin = coordinates[first] - length * first / 2;
        for (int factor = 0; factor < coordinates.length; factor++) {
            if (coordinates[factor] != null && origin + length * factor / 2 != coordinates[factor]) {
                throw new IllegalArgumentException("Over-constrained " + axis + " anchors for frame '" + id + "': conflicting positions or size");
            }
        }
        if (origin < Integer.MIN_VALUE || origin > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Anchor position exceeds integer bounds for frame '" + id + "' on " + axis + " axis");
        }
        return new Axis((int) origin, (int) length);
    }
}
