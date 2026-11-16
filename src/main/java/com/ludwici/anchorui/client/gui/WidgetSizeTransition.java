package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ludwici.anchorui.api.animation.UiAnimationSpec.Easing;
import com.ludwici.anchorui.api.animation.UiSizeInterpolator;
import com.ludwici.anchorui.api.binding.BoundNumber;
import com.ludwici.anchorui.api.binding.ProgressMath;
import com.ludwici.anchorui.api.widget.WidgetContext;

/**
 * Resolves target visual size from bindings and animates it without
 * changing the widget's layout size or stretching its texture.
 */
public final class WidgetSizeTransition {
    private record Dimension(BoundNumber value, BoundNumber maximum, UiSizeInterpolator interpolation) {
        double sample(WidgetContext context, double implicitFraction, long now) {
            double fraction = value == null ? implicitFraction : ProgressMath.fraction(value.get(context), maximum.get(context));
            return interpolation.sample(fraction, now);
        }
    }

    private final WidgetContext context;
    private final Dimension width;
    private final Dimension height;

    private WidgetSizeTransition(WidgetContext context, Dimension width, Dimension height) {
        this.context = context;
        this.width = width;
        this.height = height;
    }

    public static WidgetSizeTransition parse(JsonObject settings, WidgetContext context, boolean progressFill) {
        if (!settings.has("sizeTransition")) {
            return null;
        }
        JsonElement raw = settings.get("sizeTransition");
        if (!raw.isJsonObject()) {
            throw new IllegalArgumentException("'sizeTransition' must be an object");
        }
        JsonObject config = raw.getAsJsonObject();
        for (var entry : config.entrySet()) {
            if (!java.util.Set.of("duration", "easing", "width", "height").contains(entry.getKey())) {
                throw new IllegalArgumentException("Unknown sizeTransition property '" + entry.getKey() + "'");
            }
        }
        int duration = 400;
        if (config.has("duration")) {
            JsonElement durationElement = config.get("duration");
            if (!durationElement.isJsonPrimitive() || !durationElement.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("sizeTransition.duration must be an integer");
            }
            double number = durationElement.getAsDouble();
            if (!Double.isFinite(number) || number < 1 || number > 10000 || number != Math.floor(number)) {
                throw new IllegalArgumentException("sizeTransition.duration must be 1..10000ms");
            }
            duration = (int) number;
        }
        String ease = config.has("easing") ? config.get("easing").getAsString() : "easeOut";
        Easing easing = switch (ease) {
            case "linear" -> Easing.LINEAR;
            case "easeIn" -> Easing.EASE_IN;
            case "easeOut" -> Easing.EASE_OUT;
            case "easeInOut" -> Easing.EASE_IN_OUT;
            case "outCubic" -> Easing.OUT_CUBIC;
            default -> throw new IllegalArgumentException("Unknown sizeTransition easing '" + ease + "'");
        };
        Dimension width = dimension(config, "width", duration, easing);
        Dimension height = dimension(config, "height", duration, easing);
        if (width == null && height == null && progressFill) {
            width = new Dimension(null, null, new UiSizeInterpolator(duration, easing));
        }
        if (width == null && height == null) {
            throw new IllegalArgumentException("sizeTransition requires width/height bindings (or a progress fill)");
        }
        return new WidgetSizeTransition(context, width, height);
    }

    private static Dimension dimension(JsonObject config, String name, int duration, Easing easing) {
        if (!config.has(name)) {
            return null;
        }
        if (!config.get(name).isJsonObject()) {
            throw new IllegalArgumentException("sizeTransition." + name + " must have value and max bindings");
        }
        JsonObject source = config.getAsJsonObject(name);
        if (!source.has("value") || !source.has("max")) {
            throw new IllegalArgumentException("sizeTransition." + name + " requires value and max");
        }
        return new Dimension(BoundNumber.parse(source, "value", 0.0), BoundNumber.parse(source, "max", 1.0), new UiSizeInterpolator(duration, easing));
    }

    public double widthFraction(double implicit, long now) {
        return width == null ? 1.0 : width.sample(context, implicit, now);
    }

    public double heightFraction(double implicit, long now) {
        return height == null ? 1.0 : height.sample(context, implicit, now);
    }
}
