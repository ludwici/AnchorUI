package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.widget.WidgetIds;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class HudLayoutOffsets {
    public record Offset(int x, int y) {
        public static final Offset ZERO = new Offset(0, 0);
    }

    private final Map<String, Offset> offsets = new LinkedHashMap<>();

    public Offset get(String id) {
        return offsets.getOrDefault(id, Offset.ZERO);
    }

    public void set(String id, int x, int y) {
        String qualified = WidgetIds.id(id, "anchorui");
        if (x == 0 && y == 0) {
            offsets.remove(qualified);
        } else {
            offsets.put(qualified, new Offset(x, y));
        }
    }

    public void reset(String id) {
        offsets.remove(id);
    }

    public void resetAll() {
        offsets.clear();
    }

    public Map<String, Offset> entries() {
        return Collections.unmodifiableMap(offsets);
    }

    public HudLayoutOffsets copy() {
        HudLayoutOffsets result = new HudLayoutOffsets();
        result.offsets.putAll(offsets);
        return result;
    }

    public boolean sameAs(HudLayoutOffsets other) {
        return offsets.equals(other.offsets);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject values = new JsonObject();
        for (var entry : new TreeMap<>(offsets).entrySet()) {
            JsonObject offset = new JsonObject();
            offset.addProperty("x", entry.getValue().x());
            offset.addProperty("y", entry.getValue().y());
            values.add(entry.getKey(), offset);
        }
        root.add("offsets", values);
        return root;
    }

    public static HudLayoutOffsets fromJson(JsonObject root) {
        if (root.has("version") && root.get("version").getAsInt() != 1) {
            throw new IllegalArgumentException("Unsupported Anchor UI HUD layout version");
        }
        HudLayoutOffsets result = new HudLayoutOffsets();
        if (!root.has("offsets")) {
            return result;
        }
        JsonObject positions = root.getAsJsonObject("offsets");
        for (var entry : positions.entrySet()) {
            JsonObject value = entry.getValue().getAsJsonObject();
            result.set(entry.getKey(), value.get("x").getAsInt(), value.get("y").getAsInt());
        }
        return result;
    }
}
