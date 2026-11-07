package com.ludwici.anchorui.client.test;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record CounterConfig(int initial, int step) {
    private static final Codec<CounterConfig> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("initial", 0).forGetter(CounterConfig::initial),
            Codec.INT.optionalFieldOf("step", 1).forGetter(CounterConfig::step)
    ).apply(instance, CounterConfig::new));

    public static final Codec<CounterConfig> CODEC = BASE_CODEC.validate(config -> {
        if (config.step() <= 0) {
            return DataResult.error(() -> "step must be greater than zero");
        }
        return DataResult.success(config);
    });
}
