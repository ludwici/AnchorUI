package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.widget.WidgetContext;

import java.util.ArrayList;
import java.util.List;

public final class BoundText {
    private record Segment(String literal, BoundData value) {
    }

    private final List<Segment> segments;
    private final boolean dynamic;

    private BoundText(List<Segment> segments, boolean dynamic) {
        this.segments = List.copyOf(segments);
        this.dynamic = dynamic;
    }

    public static BoundText parse(JsonElement input) {
        if (input == null || input.isJsonNull()) {
            return new BoundText(List.of(new Segment("", null)), false);
        }
        if (input.isJsonObject()) {
            JsonObject root = new JsonObject();
            root.add("text", input);
            BoundData data = BoundData.parse(root, "text", new JsonPrimitive(""), DataBindingSpec.Kind.STRING);
            return new BoundText(List.of(new Segment("", data)), data.isBound());
        }
        if (!input.isJsonPrimitive() || !input.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Text binding must be a string or an object with 'bind'");
        }
        List<Segment> result = new ArrayList<>();
        boolean dynamic = false;
        for (TextTemplateParts.Part part : TextTemplateParts.parse(input.getAsString())) {
            if (!part.dynamic()) {
                result.add(new Segment(part.text(), null));
                continue;
            }
            JsonObject config = new JsonObject();
            config.addProperty("bind", part.source());
            config.addProperty("path", part.path());
            if (!part.format().isEmpty()) {
                config.addProperty("format", part.format());
            }
            config.addProperty("fallback", "?");
            JsonObject root = new JsonObject();
            root.add("value", config);
            result.add(new Segment("", BoundData.parse(root, "value", new JsonPrimitive("?"), DataBindingSpec.Kind.ANY)));
            dynamic = true;
        }
        return new BoundText(result, dynamic);
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public String get(WidgetContext context) {
        StringBuilder result = new StringBuilder();
        for (Segment segment : segments) {
            if (segment.value() == null) {
                result.append(segment.literal());
                continue;
            }
            JsonElement value = segment.value().get(context);
            String rendered = value.isJsonPrimitive() ? value.getAsString() : value.toString();
            if (rendered.length() > 512) {
                rendered = rendered.substring(0, 512);
            }
            result.append(rendered.replace("<", "‹").replace(">", "›"));
            if (result.length() > 4096) {
                return result.substring(0, 4096);
            }
        }
        return result.toString();
    }
}
