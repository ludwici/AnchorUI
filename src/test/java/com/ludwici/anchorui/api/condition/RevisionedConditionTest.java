package com.ludwici.anchorui.api.condition;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.binding.DataValueRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RevisionedConditionTest {
    @Test
    public void conditionReusesVersionedSourceAcrossFramesAndMultiplePaths() {
        ResourceLocation id = ResourceLocation.parse("revision_condition_test:stats");
        AtomicInteger reads = new AtomicInteger();
        AtomicLong revision = new AtomicLong();
        AtomicInteger rank = new AtomicInteger(3);
        DataValueRegistry.registerRevisioned(id, context -> revision.get(), context -> {
            reads.incrementAndGet();
            return JsonParser.parseString("{\"stats\":{\"rank\":" + rank.get() + ",\"enabled\":true}}");
        });
        BoundCondition condition = BoundCondition.parse(JsonParser.parseString("""
                {
                  "visibleWhen": {
                    "all": [
                      {"left":{"bind":"revision_condition_test:stats","path":"stats.rank"},"op":"gte","right":5},
                      {"left":{"bind":"revision_condition_test:stats","path":"stats.enabled"},"op":"eq","right":true}
                    ]
                  }
                }
                """).getAsJsonObject(), "visibleWhen", false);
        assertFalse(condition.get(null));
        assertFalse(condition.get(null));
        assertEquals(1, reads.get());

        rank.set(8);
        assertFalse(condition.get(null));
        revision.incrementAndGet();
        assertTrue(condition.get(null));
        assertTrue(condition.get(null));
        assertEquals(2, reads.get());
    }

    @Test
    public void liveConditionStillReadsProviderOnEveryEvaluation() {
        ResourceLocation id = ResourceLocation.parse("revision_condition_test:live");
        AtomicInteger reads = new AtomicInteger();
        DataValueRegistry.register(id, context -> new JsonPrimitive(reads.incrementAndGet()));
        BoundCondition condition = BoundCondition.parse(JsonParser.parseString("""
                {"enabledWhen":{"left":{"bind":"revision_condition_test:live"},"op":"gte","right":2}}
                """).getAsJsonObject(), "enabledWhen", false);
        assertFalse(condition.get(null));
        assertTrue(condition.get(null));
        assertEquals(2, reads.get());
    }
}
