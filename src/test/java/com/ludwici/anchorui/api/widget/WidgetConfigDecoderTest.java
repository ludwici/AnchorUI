package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonParser;
import com.ludwici.anchorui.client.test.CounterConfig;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WidgetConfigDecoderTest {
    @Test
    public void decodesRecordWithDefaults() {
        var json = JsonParser.parseString("{\"type\":\"anchorui_test:counter\",\"id\":\"default\"}");
        CounterConfig result = WidgetConfigDecoder.decode(CounterConfig.CODEC, json);
        assertEquals(0, result.initial());
        assertEquals(1, result.step());
    }

    @Test
    public void decodesCustomFieldsAndIgnoresLayoutFields() {
        var json = JsonParser.parseString("{\"type\":\"anchorui_test:counter\",\"initial\":10,\"step\":5,\"size\":{\"width\":120,\"height\":20}}");
        CounterConfig result = WidgetConfigDecoder.decode(CounterConfig.CODEC, json);
        assertEquals(10, result.initial());
        assertEquals(5, result.step());
    }

    @Test
    public void rejectsWrongFieldType() {
        var json = JsonParser.parseString("{\"step\":\"invalid\"}");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> WidgetConfigDecoder.decode(CounterConfig.CODEC, json));
        assertTrue(exception.getMessage().contains("step"));
    }

    @Test
    public void rejectsOutOfRangeValues() {
        var json = JsonParser.parseString("{\"step\":0}");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> WidgetConfigDecoder.decode(CounterConfig.CODEC, json));
        assertTrue(exception.getMessage().contains("step"));
    }

    @Test
    public void encodesAndDecodesRecords() {
        CounterConfig original = new CounterConfig(8, 3);
        var json = CounterConfig.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        assertEquals(original, WidgetConfigDecoder.decode(CounterConfig.CODEC, json));
    }
}
