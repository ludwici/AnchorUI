package com.ludwici.anchorui.api.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.UiIdentifiers;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class UiAnimationSpec {
    public enum Trigger {
        OPEN, SHOW, HIDE, CHANGE, CLOSE, CUSTOM
    }

    public enum Easing {
        LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, OUT_CUBIC;

        public double apply(double progress) {
            double t = Math.max(0.0, Math.min(1.0, progress));
            return switch (this) {
                case LINEAR -> t;
                case EASE_IN -> t * t;
                case EASE_OUT -> 1.0 - (1.0 - t) * (1.0 - t);
                case EASE_IN_OUT -> t < 0.5 ? 2.0 * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 2.0) / 2.0;
                case OUT_CUBIC -> 1.0 - Math.pow(1.0 - t, 3.0);
            };
        }
    }

    public record Frame(double opacity, double x, double y, double scale) {
        public static final Frame IDENTITY = new Frame(1.0, 0.0, 0.0, 1.0);

        public Frame {
            if (!Double.isFinite(opacity) || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(scale)
                    || opacity < 0.0 || opacity > 1.0 || scale <= 0.0 || scale > 4.0
                    || x < -4096.0 || x > 4096.0 || y < -4096.0 || y > 4096.0) {
                throw new IllegalArgumentException("Invalid animation frame (opacity 0..1, x/y -4096..4096, scale >0..4)");
            }
        }

        public Frame between(Frame end, double t) {
            double p = Math.max(0.0, Math.min(1.0, t));
            return new Frame(opacity + (end.opacity - opacity) * p, x + (end.x - x) * p, y + (end.y - y) * p, scale + (end.scale - scale) * p);
        }

        public double inverseX(double value, double centerX) {
            return (value - x - centerX) / scale + centerX;
        }

        public double inverseY(double value, double centerY) {
            return (value - y - centerY) / scale + centerY;
        }
    }

    public record Track(String id, Trigger trigger, ResourceLocation customTrigger, long durationMillis, long delayMillis, Easing easing,
                        Frame from, Frame to, JsonElement watch, boolean enabled) {
        public Track {
            if (id == null || !id.matches("[A-Za-z][A-Za-z0-9_-]*")) {
                throw new IllegalArgumentException("Animation id must start with a letter and contain letters, digits, _ or -");
            }
            if (durationMillis < 1 || durationMillis > 10000 || delayMillis < 0 || delayMillis > 60000) {
                throw new IllegalArgumentException("Animation duration must be 1..10000ms and delay must be 0..60000ms");
            }
            if ((trigger == Trigger.CUSTOM) != (customTrigger != null)) {
                throw new IllegalArgumentException("Custom animation '" + id + "' requires a namespaced trigger ID");
            }
            if (trigger == Trigger.CHANGE && watch == null && enabled) {
                throw new IllegalArgumentException("Animation '" + id + "' with trigger change requires watch");
            }
            if (trigger != Trigger.CHANGE && watch != null) {
                throw new IllegalArgumentException("Only change animation '" + id + "' can declare watch");
            }
            watch = watch == null ? null : watch.deepCopy();
        }

        public long totalMillis() {
            return delayMillis + durationMillis;
        }
    }

    private final List<Track> tracks;

    private UiAnimationSpec(List<Track> tracks) {
        this.tracks = List.copyOf(tracks);
    }

    public static UiAnimationSpec parse(JsonObject settings) {
        return parse(settings, null);
    }

    public static UiAnimationSpec parse(JsonObject settings, JsonArray defaults) {
        if (settings.has("animation")) {
            throw new IllegalArgumentException("Unsupported 'animation' object; use the 'animations' array");
        }
        JsonArray resolved = settings.has("animations") ? array(settings) : defaults == null ? new JsonArray() : defaults.deepCopy();
        return new UiAnimationSpec(parseEntries(merge(null, resolved)));
    }

    public static JsonArray array(JsonObject settings) {
        if (!settings.has("animations")) {
            return new JsonArray();
        }
        if (!settings.get("animations").isJsonArray()) {
            throw new IllegalArgumentException("'animations' must be a JSON array");
        }
        return settings.getAsJsonArray("animations").deepCopy();
    }

    public static JsonArray merge(JsonArray defaults, JsonArray local) {
        Map<String, JsonObject> byId = new LinkedHashMap<>();
        addMerged(byId, defaults, "document");
        addMerged(byId, local, "widget");
        JsonArray result = new JsonArray();
        byId.values().forEach(result::add);
        return result;
    }

    private static void addMerged(Map<String, JsonObject> target, JsonArray entries, String source) {
        if (entries == null) {
            return;
        }
        Set<String> encountered = new java.util.HashSet<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Animation in " + source + " must be an object");
            }
            JsonObject object = element.getAsJsonObject();
            String id = string(object, "id", null);
            if (id == null || !id.matches("[A-Za-z][A-Za-z0-9_-]*")) {
                throw new IllegalArgumentException("Animation id in " + source + " must start with a letter and contain letters, digits, _ or -");
            }
            if (!encountered.add(id)) {
                throw new IllegalArgumentException("Duplicate animation id '" + id + "' in " + source);
            }
            target.put(id, object.deepCopy());
        }
    }

    private static List<Track> parseEntries(JsonArray entries) {
        List<Track> result = new ArrayList<>();
        for (JsonElement element : entries) {
            JsonObject value = element.getAsJsonObject();
            String id = string(value, "id", null);
            for (var entry : value.entrySet()) {
                if (!Set.of("id", "trigger", "duration", "delay", "easing", "preset", "from", "to", "watch", "enabled").contains(entry.getKey())) {
                    throw new IllegalArgumentException("Unknown property '" + entry.getKey() + "' in animation '" + id + "'");
                }
            }
            String event = string(value, "trigger", "open");
            Trigger trigger = switch (event) {
                case "open" -> Trigger.OPEN;
                case "show" -> Trigger.SHOW;
                case "hide" -> Trigger.HIDE;
                case "change" -> Trigger.CHANGE;
                case "close" -> Trigger.CLOSE;
                default -> Trigger.CUSTOM;
            };
            ResourceLocation customTrigger = null;
            if (trigger == Trigger.CUSTOM) {
                customTrigger = UiIdentifiers.requireNamespaced(event, "custom trigger for animation '" + id + "'");
            }
            long duration = integer(value, "duration", 300, 1, 10000);
            long delay = integer(value, "delay", 0, 0, 60000);
            Easing easing = switch (string(value, "easing", "linear")) {
                case "linear" -> Easing.LINEAR;
                case "easeIn" -> Easing.EASE_IN;
                case "easeOut" -> Easing.EASE_OUT;
                case "easeInOut" -> Easing.EASE_IN_OUT;
                case "outCubic" -> Easing.OUT_CUBIC;
                default -> throw new IllegalArgumentException("Invalid easing in animation '" + id + "'");
            };
            String preset = string(value, "preset", "custom");
            if (!Set.of("custom", "fade", "slideLeft", "slideRight", "slideUp", "zoom").contains(preset)) {
                throw new IllegalArgumentException("Invalid preset '" + preset + "' in animation '" + id + "'");
            }
            boolean exiting = trigger == Trigger.HIDE || trigger == Trigger.CLOSE;
            Frame defaultsFrom = switch (preset) {
                case "fade" -> exiting ? Frame.IDENTITY : new Frame(0, 0, 0, 1);
                case "slideLeft" -> new Frame(1, exiting ? 0 : -32, 0, 1);
                case "slideRight" -> new Frame(1, exiting ? 0 : 32, 0, 1);
                case "slideUp" -> new Frame(1, 0, exiting ? 0 : 24, 1);
                case "zoom" -> new Frame(1, 0, 0, exiting ? 1 : 0.82);
                default -> Frame.IDENTITY;
            };
            Frame defaultsTo = switch (preset) {
                case "fade" -> exiting ? new Frame(0, 0, 0, 1) : Frame.IDENTITY;
                case "slideLeft" -> new Frame(1, exiting ? -32 : 0, 0, 1);
                case "slideRight" -> new Frame(1, exiting ? 32 : 0, 0, 1);
                case "slideUp" -> new Frame(1, 0, exiting ? -24 : 0, 1);
                case "zoom" -> new Frame(1, 0, 0, exiting ? 0.82 : 1);
                default -> Frame.IDENTITY;
            };
            Frame from = frame(value, "from", defaultsFrom);
            Frame to = frame(value, "to", defaultsTo);
            boolean enabled = booleanValue(value, "enabled", true);
            result.add(new Track(id, trigger, customTrigger, duration, delay, easing, from, to, value.get("watch"), enabled));
        }
        return result;
    }

    private static Frame frame(JsonObject value, String property, Frame fallback) {
        if (!value.has(property)) {
            return fallback;
        }
        JsonElement raw = value.get(property);
        if (!raw.isJsonObject()) {
            throw new IllegalArgumentException("'" + property + "' must be an object");
        }
        JsonObject object = raw.getAsJsonObject();
        for (var entry : object.entrySet()) {
            if (!Set.of("opacity", "x", "y", "scale").contains(entry.getKey())) {
                throw new IllegalArgumentException("Invalid frame field '" + entry.getKey() + "'");
            }
        }
        return new Frame(number(object, "opacity", fallback.opacity()), number(object, "x", fallback.x()), number(object, "y", fallback.y()), number(object, "scale", fallback.scale()));
    }

    private static String string(JsonObject value, String key, String fallback) {
        if (!value.has(key)) {
            return fallback;
        }
        JsonElement raw = value.get(key);
        if (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("'" + key + "' must be a string");
        }
        return raw.getAsString();
    }

    private static long integer(JsonObject value, String key, long fallback, long min, long max) {
        if (!value.has(key)) {
            return fallback;
        }
        JsonElement raw = value.get(key);
        if (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("'" + key + "' must be an integer");
        }
        double number = raw.getAsDouble();
        if (!Double.isFinite(number) || number != Math.floor(number) || number < min || number > max) {
            throw new IllegalArgumentException("'" + key + "' must be between " + min + " and " + max);
        }
        return (long) number;
    }

    private static double number(JsonObject value, String key, double fallback) {
        if (!value.has(key)) {
            return fallback;
        }
        JsonElement raw = value.get(key);
        if (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("'" + key + "' must be a number");
        }
        return raw.getAsDouble();
    }

    private static boolean booleanValue(JsonObject value, String key, boolean fallback) {
        if (!value.has(key)) {
            return fallback;
        }
        JsonElement raw = value.get(key);
        if (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("'" + key + "' must be true or false");
        }
        return raw.getAsBoolean();
    }

    public List<Track> tracks() {
        return tracks;
    }

    public boolean isEmpty() {
        return tracks.stream().noneMatch(Track::enabled);
    }
}
