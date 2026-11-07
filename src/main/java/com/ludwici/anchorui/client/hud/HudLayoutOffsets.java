package com.ludwici.anchorui.client.hud;

import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.widget.WidgetIds;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class HudLayoutOffsets {
    public record Offset(int x, int y) {
        public static final Offset ZERO = new Offset(0, 0);
    }

    public record AnchorOverride(String relativeTo, ElementAnchor point, ElementAnchor anchor) {
        public AnchorOverride {
            relativeTo = WidgetIds.reference(relativeTo, "anchorui");
            if (point == null || anchor == null) {
                throw new IllegalArgumentException("HUD anchor points must not be null");
            }
        }
    }

    private final Map<String, Offset> offsets = new LinkedHashMap<>();
    private final Map<String, AnchorOverride> anchors = new LinkedHashMap<>();
    private final Set<String> hidden = new TreeSet<>();

    public Offset get(String id) {
        return offsets.getOrDefault(id, Offset.ZERO);
    }

    public AnchorOverride anchor(String id) {
        return anchors.get(id);
    }

    public boolean isHidden(String id) {
        return hidden.contains(id);
    }

    public void set(String id, int x, int y) {
        String qualified = WidgetIds.id(id, "anchorui");
        if (x == 0 && y == 0) {
            offsets.remove(qualified);
        } else {
            offsets.put(qualified, new Offset(x, y));
        }
    }

    public void setAnchor(String id, AnchorOverride anchor) {
        anchors.put(WidgetIds.id(id, "anchorui"), anchor);
    }

    public void setHidden(String id, boolean value) {
        String qualified = WidgetIds.id(id, "anchorui");
        if (value) {
            hidden.add(qualified);
        } else {
            hidden.remove(qualified);
        }
    }

    public void reset(String id) {
        offsets.remove(id);
        anchors.remove(id);
        hidden.remove(id);
    }

    public void resetAll() {
        offsets.clear();
        anchors.clear();
        hidden.clear();
    }

    public Map<String, Offset> entries() {
        return Collections.unmodifiableMap(offsets);
    }

    public HudLayoutOffsets copy() {
        HudLayoutOffsets result = new HudLayoutOffsets();
        result.offsets.putAll(offsets);
        result.anchors.putAll(anchors);
        result.hidden.addAll(hidden);
        return result;
    }

    public boolean sameAs(HudLayoutOffsets other) {
        return offsets.equals(other.offsets) && anchors.equals(other.anchors) && hidden.equals(other.hidden);
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject values = new JsonObject();
        Set<String> ids = new TreeSet<>();
        ids.addAll(offsets.keySet());
        ids.addAll(anchors.keySet());
        ids.addAll(hidden);
        for (String id : ids) {
            JsonObject value = new JsonObject();
            Offset offset = get(id);
            if (!Offset.ZERO.equals(offset)) {
                value.addProperty("x", offset.x());
                value.addProperty("y", offset.y());
            }
            AnchorOverride anchor = anchors.get(id);
            if (anchor != null) {
                value.addProperty("relativeTo", anchor.relativeTo());
                value.addProperty("point", anchor.point().name());
                value.addProperty("anchor", anchor.anchor().name());
            }
            if (hidden.contains(id)) {
                value.addProperty("hidden", true);
            }
            values.add(id, value);
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
            String id = WidgetIds.id(entry.getKey(), "anchorui");
            JsonObject value = entry.getValue().getAsJsonObject();
            int x = value.has("x") ? value.get("x").getAsInt() : 0;
            int y = value.has("y") ? value.get("y").getAsInt() : 0;
            result.set(id, x, y);
            if (value.has("relativeTo")) {
                String target = value.get("relativeTo").getAsString();
                ElementAnchor point = ElementAnchor.fromString(value.get("point").getAsString());
                ElementAnchor anchor = ElementAnchor.fromString(value.get("anchor").getAsString());
                result.setAnchor(id, new AnchorOverride(target, point, anchor));
            }
            if (value.has("hidden")) {
                result.setHidden(id, value.get("hidden").getAsBoolean());
            }
        }
        return result;
    }
}
