package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.animation.UiAnimationSpec;
import com.ludwici.anchorui.api.widget.WidgetEditorRegistry;

import java.util.ArrayList;
import java.util.List;

public final class AnimationInspectorControls {
    private AnimationInspectorControls() {
    }

    public static JsonArray create(JsonArray current) {
        JsonArray result = current.deepCopy();
        JsonObject track = new JsonObject();
        track.addProperty("id", nextId(result));
        track.addProperty("trigger", "open");
        track.addProperty("preset", "fade");
        track.addProperty("duration", 300);
        track.addProperty("delay", 0);
        track.addProperty("easing", "easeOut");
        result.add(track);
        return validated(result);
    }

    private static String nextId(JsonArray tracks) {
        int index = 1;
        while (true) {
            String name = "anim" + index++;
            boolean exists = false;
            for (JsonElement track : tracks) {
                if (track.getAsJsonObject().get("id").getAsString().equals(name)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                return name;
            }
        }
    }

    public static JsonArray duplicate(JsonArray current, int index) {
        List<JsonObject> tracks = list(current);
        JsonObject copy = tracks.get(index).deepCopy();
        copy.addProperty("id", nextId(current));
        tracks.add(index + 1, copy);
        return validated(array(tracks));
    }

    public static JsonArray delete(JsonArray current, int index) {
        List<JsonObject> tracks = list(current);
        tracks.remove(index);
        return validated(array(tracks));
    }

    public static JsonArray move(JsonArray current, int index, int direction) {
        List<JsonObject> tracks = list(current);
        int next = index + direction;
        if (next >= 0 && next < tracks.size()) {
            java.util.Collections.swap(tracks, index, next);
        }
        return validated(array(tracks));
    }

    public static boolean sameTracks(JsonArray left, JsonArray right) {
        return UiAnimationSpec.parse(document(left)).tracks().equals(UiAnimationSpec.parse(document(right)).tracks());
    }

    private static JsonObject document(JsonArray tracks) {
        JsonObject json = new JsonObject();
        json.add("animations", tracks);
        return json;
    }

    public static List<AppearanceInspectorControls.Control> describe(JsonArray tracks) {
        return describe(tracks, tracks);
    }

    public static List<AppearanceInspectorControls.Control> describe(JsonArray tracks, JsonArray source) {
        List<AppearanceInspectorControls.Control> result = new ArrayList<>();
        for (int index = 0; index < tracks.size(); index++) {
            JsonObject track = tracks.get(index).getAsJsonObject();
            JsonObject original = null;
            for (JsonElement candidate : source) {
                JsonObject entry = candidate.getAsJsonObject();
                if (entry.get("id").getAsString().equals(track.get("id").getAsString())) {
                    original = entry;
                    break;
                }
            }
            String prefix = "animations.t" + index + ".";
            result.add(new AppearanceInspectorControls.Control(WidgetEditorRegistry.Field.text(prefix + "id", "Animation ID", ""), track.get("id").getAsString(), original == null, original == null ? "" : original.get("id").getAsString()));
            add(result, track, original, WidgetEditorRegistry.Field.text(prefix + "trigger", "Trigger ID", "open"), "trigger");
            add(result, track, original, WidgetEditorRegistry.Field.choice(prefix + "preset", "Effect", "custom", "custom", "fade", "slideLeft", "slideRight", "slideUp", "zoom"), "preset");
            add(result, track, original, WidgetEditorRegistry.Field.integer(prefix + "duration", "Duration (ms)", 300, 1, 10000, 50), "duration");
            add(result, track, original, WidgetEditorRegistry.Field.integer(prefix + "delay", "Delay (ms)", 0, 0, 60000, 50), "delay");
            add(result, track, original, WidgetEditorRegistry.Field.choice(prefix + "easing", "Easing", "linear", "linear", "easeIn", "easeOut", "easeInOut", "outCubic"), "easing");
            add(result, track, original, WidgetEditorRegistry.Field.bool(prefix + "enabled", "Enabled", true), "enabled");
            if ("change".equals(value(track, "trigger", "open"))) {
                String watch = track.has("watch") ? track.get("watch").toString() : "";
                String baseline = original != null && original.has("watch") ? original.get("watch").toString() : "";
                result.add(new AppearanceInspectorControls.Control(WidgetEditorRegistry.Field.text(prefix + "watch", "Watch binding JSON", ""), watch, !watch.equals(baseline), baseline));
            }
            if ("custom".equals(value(track, "preset", "custom"))) {
                for (String endpoint : List.of("from", "to")) {
                    JsonObject keyframe = track.has(endpoint) ? track.getAsJsonObject(endpoint) : new JsonObject();
                    JsonObject originalFrame = original != null && original.has(endpoint) ? original.getAsJsonObject(endpoint) : null;
                    add(result, keyframe, originalFrame, WidgetEditorRegistry.Field.number(prefix + endpoint + ".opacity", endpoint + " opacity", 1, 0, 1, 0.1), "opacity");
                    add(result, keyframe, originalFrame, WidgetEditorRegistry.Field.number(prefix + endpoint + ".x", endpoint + " X", 0, -4096, 4096, 1), "x");
                    add(result, keyframe, originalFrame, WidgetEditorRegistry.Field.number(prefix + endpoint + ".y", endpoint + " Y", 0, -4096, 4096, 1), "y");
                    add(result, keyframe, originalFrame, WidgetEditorRegistry.Field.number(prefix + endpoint + ".scale", endpoint + " scale", 1, 0.01, 4, 0.05), "scale");
                }
            }
        }
        return List.copyOf(result);
    }

    private static void add(List<AppearanceInspectorControls.Control> controls, JsonObject parent, JsonObject original, WidgetEditorRegistry.Field field, String property) {
        String text = value(parent, property, field.fallback().getAsString());
        String source = original == null ? field.fallback().getAsString() : value(original, property, field.fallback().getAsString());
        controls.add(new AppearanceInspectorControls.Control(field, text, !AppearanceInspectorControls.sameValue(field, text, source), source));
    }

    private static String value(JsonObject obj, String property, String fallback) {
        JsonElement existing = obj.get(property);
        return existing != null && existing.isJsonPrimitive() ? existing.getAsString() : fallback;
    }

    public static JsonArray set(JsonArray current, AppearanceInspectorControls.Control control, String value) {
        JsonArray result = current.deepCopy();
        String[] path = control.field().path().split("\\.");
        if (path.length < 3 || path.length > 4 || !path[0].equals("animations") || !path[1].matches("t[0-9]+")) {
            throw new IllegalArgumentException("Invalid animation field path");
        }
        int index = Integer.parseInt(path[1].substring(1));
        JsonObject track = result.get(index).getAsJsonObject();
        String property = path[path.length - 1];
        if (property.equals("watch")) {
            if (value.isBlank()) {
                track.remove("watch");
            } else {
                JsonElement json = JsonParser.parseString(value);
                if (!json.isJsonObject()) {
                    throw new IllegalArgumentException("Animation watch must be a JSON object");
                }
                track.add("watch", json);
            }
        } else {
            JsonElement parsed = AppearanceInspectorControls.parse(control.field(), value);
            JsonObject container = track;
            if (path.length == 4) {
                if (!track.has(path[2])) {
                    track.add(path[2], new JsonObject());
                }
                container = track.getAsJsonObject(path[2]);
            }
            container.add(property, parsed);
            if (property.equals("preset")) {
                track.remove("from");
                track.remove("to");
            }
            if (property.equals("trigger")) {
                if ("change".equals(value) && !track.has("watch")) {
                    track.addProperty("enabled", false);
                }
                if (!"change".equals(value)) {
                    track.remove("watch");
                }
            }
        }
        return validated(result);
    }

    public static JsonArray reset(JsonArray current, AppearanceInspectorControls.Control control) {
        return reset(current, control, new JsonArray());
    }

    public static JsonArray reset(JsonArray current, AppearanceInspectorControls.Control control, JsonArray source) {
        JsonArray result = current.deepCopy();
        String[] path = control.field().path().split("\\.");
        int index = Integer.parseInt(path[1].substring(1));
        JsonObject track = result.get(index).getAsJsonObject();
        JsonObject original = null;
        for (JsonElement entry : source) {
            JsonObject sourceTrack = entry.getAsJsonObject();
            if (sourceTrack.get("id").getAsString().equals(track.get("id").getAsString())) {
                original = sourceTrack;
                break;
            }
        }
        if (path.length == 3) {
            if (path[2].equals("id")) {
                return result;
            }
            if (original != null && original.has(path[2])) {
                track.add(path[2], original.get(path[2]).deepCopy());
            } else {
                track.remove(path[2]);
            }
            if (path[2].equals("trigger")) {
                if (original != null && original.has("watch")) {
                    track.add("watch", original.get("watch").deepCopy());
                } else {
                    track.remove("watch");
                }
            }
        } else {
            JsonObject frame = track.has(path[2]) ? track.getAsJsonObject(path[2]) : new JsonObject();
            if (original != null && original.has(path[2]) && original.getAsJsonObject(path[2]).has(path[3])) {
                frame.add(path[3], original.getAsJsonObject(path[2]).get(path[3]).deepCopy());
            } else {
                frame.remove(path[3]);
            }
            if (frame.isEmpty()) {
                track.remove(path[2]);
            } else {
                track.add(path[2], frame);
            }
        }
        return validated(result);
    }

    public static JsonArray validated(JsonArray tracks) {
        JsonObject root = new JsonObject();
        root.add("animations", tracks);
        UiAnimationSpec.parse(root);
        return tracks;
    }

    private static List<JsonObject> list(JsonArray source) {
        List<JsonObject> tracks = new ArrayList<>();
        for (JsonElement track : source) {
            tracks.add(track.getAsJsonObject().deepCopy());
        }
        return tracks;
    }

    private static JsonArray array(List<JsonObject> tracks) {
        JsonArray result = new JsonArray();
        for (JsonObject track : tracks) {
            result.add(track);
        }
        return result;
    }
}
