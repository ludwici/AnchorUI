package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public final class BuiltinDataFormatters {
    private BuiltinDataFormatters() {
    }

    public static void register() {
        DataFormatterRegistry.register(id("integer"), (value, context) ->
                new JsonPrimitive(Long.toString(Math.round(number(value)))));
        DataFormatterRegistry.register(id("percent"), (value, context) ->
                new JsonPrimitive(Math.round(number(value) * 100.0) + "%"));
        DataFormatterRegistry.register(id("compact"), (value, context) ->
                new JsonPrimitive(compact(number(value))));
        DataFormatterRegistry.register(id("uppercase"), (value, context) -> {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("uppercase expects a string");
            }
            return new JsonPrimitive(value.getAsString().toUpperCase(Locale.ROOT));
        });
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("anchorui", path);
    }

    private static double number(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Numeric formatter expects a number");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("Numeric formatter expects a finite number");
        }
        return number;
    }

    private static String compact(double number) {
        double absolute = Math.abs(number);
        if (absolute < 1000) {
            return Long.toString(Math.round(number));
        }
        String suffix;
        double divisor;
        if (absolute >= 1_000_000_000) {
            divisor = 1_000_000_000.0;
            suffix = "B";
        } else if (absolute >= 1_000_000) {
            divisor = 1_000_000.0;
            suffix = "M";
        } else {
            divisor = 1000.0;
            suffix = "K";
        }
        double scaled = number / divisor;
        String text = Math.abs(scaled) >= 10 ? Long.toString(Math.round(scaled))
                : String.format(Locale.ROOT, "%.1f", scaled).replaceAll("\\.0$", "");
        return text + suffix;
    }
}
