package com.ludwici.anchorui.api.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumMap;
import java.util.Map;

public final class UiAnimationSpec {
    public enum Trigger {
        OPEN, SHOW, HIDE, CHANGE, CLOSE
    }

    public enum Easing {
        LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, OUT_CUBIC;

        public double apply(double progress) {
            double t = Math.max(0.0, Math.min(1.0, progress));
            return switch (this) {
                case LINEAR -> t;
                case EASE_IN -> t * t;
                case EASE_OUT -> 1.0 - (1.0 - t) * (1.0 - t);
                case EASE_IN_OUT -> t < 0.5 ? 2.0 * t * t
                        : 1.0 - Math.pow(-2.0 * t + 2.0, 2.0) / 2.0;
                case OUT_CUBIC -> 1.0 - Math.pow(1.0 - t, 3.0);
            };
        }
    }

    public record Frame(double opacity, double x, double y, double scale) {
        public static final Frame IDENTITY = new Frame(1.0, 0.0, 0.0, 1.0);

        public Frame {
            if (!Double.isFinite(opacity) || !Double.isFinite(x) || !Double.isFinite(y)
                    || !Double.isFinite(scale) || opacity < 0.0 || opacity > 1.0
                    || scale <= 0.0 || scale > 4.0) {
                throw new IllegalArgumentException("Invalid animation frame; opacity must be 0..1 and scale >0..4");
            }
        }

        public Frame between(Frame end, double t) {
            double p = Math.max(0.0, Math.min(1.0, t));
            return new Frame(opacity + (end.opacity - opacity) * p,
                    x + (end.x - x) * p, y + (end.y - y) * p,
                    scale + (end.scale - scale) * p);
        }

        public double inverseX(double x, double centerX) {
            return (x - this.x - centerX) / scale + centerX;
        }

        public double inverseY(double y, double centerY) {
            return (y - this.y - centerY) / scale + centerY;
        }
    }

    public record Track(long durationMillis, Easing easing, Frame from, Frame to) {
        public Track {
            if (durationMillis < 1 || durationMillis > 10000) {
                throw new IllegalArgumentException("Animation duration must be 1..10000 ms");
            }
        }

        public Frame frameAt(long elapsedNanos) {
            double t = Math.max(0.0, Math.min(1.0, elapsedNanos / (durationMillis * 1000000.0)));
            return from.between(to, easing.apply(t));
        }
    }

    private final Map<Trigger, Track> tracks;
    private final JsonElement watch;

    private UiAnimationSpec(Map<Trigger, Track> tracks, JsonElement watch) {
        this.tracks = Map.copyOf(tracks);
        this.watch = watch == null ? null : watch.deepCopy();
    }

    public static UiAnimationSpec parse(JsonObject settings) {
        return parse(settings, null);
    }

    public static UiAnimationSpec parse(JsonObject settings, JsonObject defaults) {
        JsonObject config = settings.has("animation") && settings.get("animation").isJsonObject()
                ? settings.getAsJsonObject("animation") : null;
        if (settings.has("animation") && config == null) {
            throw new IllegalArgumentException("'animation' must be a JSON object");
        }
        JsonObject result = defaults == null ? new JsonObject() : defaults.deepCopy();
        if (config != null) {
            for (var entry : config.entrySet()) {
                result.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }
        EnumMap<Trigger, Track> parsed = new EnumMap<>(Trigger.class);
        for (var entry : result.entrySet()) {
            String key = entry.getKey();
            if (key.equals("watch")) {
                continue;
            }
            Trigger trigger = switch (key) {
                case "open" -> Trigger.OPEN;
                case "show" -> Trigger.SHOW;
                case "hide" -> Trigger.HIDE;
                case "change" -> Trigger.CHANGE;
                case "close" -> Trigger.CLOSE;
                default -> throw new IllegalArgumentException("Unknown animation trigger '" + key + "'");
            };
            if (!entry.getValue().isJsonObject()) {
                throw new IllegalArgumentException("animation." + key + " must be an object");
            }
            parsed.put(trigger, parseTrack(entry.getValue().getAsJsonObject(), key));
        }
        JsonElement watch = result.get("watch");
        if (watch != null && !parsed.containsKey(Trigger.CHANGE)) {
            throw new IllegalArgumentException("animation.watch requires animation.change");
        }
        if (parsed.containsKey(Trigger.CHANGE) && watch == null) {
            throw new IllegalArgumentException("animation.change requires animation.watch");
        }
        return new UiAnimationSpec(parsed, watch);
    }

    private static Track parseTrack(JsonObject raw, String trigger) {
        for (var entry : raw.entrySet()) {
            if (!switch (entry.getKey()) {
                case "duration", "easing", "opacity", "x", "y", "scale" -> true;
                default -> false;
            }) {
                throw new IllegalArgumentException("Unknown animation." + trigger + " property '" + entry.getKey() + "'");
            }
        }
        if (!raw.has("duration") || !raw.get("duration").isJsonPrimitive()
                || !raw.getAsJsonPrimitive("duration").isNumber()) {
            throw new IllegalArgumentException("animation." + trigger + ".duration requires milliseconds");
        }
        double ms = raw.get("duration").getAsDouble();
        if (!Double.isFinite(ms) || ms < 1 || ms > 10000 || ms != Math.floor(ms)) {
            throw new IllegalArgumentException("animation." + trigger + ".duration must be 1..10000 integer milliseconds");
        }
        String easingName = raw.has("easing") ? raw.get("easing").getAsString() : "linear";
        Easing easing = switch (easingName) {
            case "linear" -> Easing.LINEAR;
            case "easeIn" -> Easing.EASE_IN;
            case "easeOut" -> Easing.EASE_OUT;
            case "easeInOut" -> Easing.EASE_IN_OUT;
            case "outCubic" -> Easing.OUT_CUBIC;
            default -> throw new IllegalArgumentException("Unknown animation easing '" + easingName + "'");
        };
        double[] opacity = pair(raw, "opacity", 1.0, 0.0, 1.0);
        double[] x = pair(raw, "x", 0.0, -4096.0, 4096.0);
        double[] y = pair(raw, "y", 0.0, -4096.0, 4096.0);
        double[] scale = pair(raw, "scale", 1.0, 0.01, 4.0);
        return new Track((long) ms, easing,
                new Frame(opacity[0], x[0], y[0], scale[0]),
                new Frame(opacity[1], x[1], y[1], scale[1]));
    }

    private static double[] pair(JsonObject root, String name, double fallback, double min, double max) {
        if (!root.has(name)) {
            return new double[]{fallback, fallback};
        }
        JsonElement value = root.get(name);
        if (!value.isJsonArray() || value.getAsJsonArray().size() != 2) {
            throw new IllegalArgumentException("animation." + name + " needs [from, to]");
        }
        double[] result = new double[2];
        for (int i = 0; i < 2; i++) {
            JsonElement raw = value.getAsJsonArray().get(i);
            if (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("animation." + name + " values must be numbers");
            }
            result[i] = raw.getAsDouble();
            if (!Double.isFinite(result[i]) || result[i] < min || result[i] > max) {
                throw new IllegalArgumentException("animation." + name + " values must be " + min + ".." + max);
            }
        }
        return result;
    }

    public Track track(Trigger trigger) {
        return tracks.get(trigger);
    }

    public JsonElement watch() {
        return watch == null ? null : watch.deepCopy();
    }

    public boolean isEmpty() {
        return tracks.isEmpty();
    }
}
