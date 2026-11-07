package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import java.util.Objects;

public final class WidgetConfigDecoder {
    private WidgetConfigDecoder() {
    }

    public static <T> T decode(Codec<T> codec, JsonElement json) {
        Objects.requireNonNull(codec, "Widget configuration codec must not be null");
        Objects.requireNonNull(json, "Widget configuration JSON must not be null");
        return codec.parse(JsonOps.INSTANCE, json).getOrThrow(IllegalArgumentException::new);
    }
}
