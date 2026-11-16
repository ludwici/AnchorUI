package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RevisionedDataBindingTest {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("revision_test", path);
    }

    private static JsonObject json(String source, String field) {
        return JsonParser.parseString("{\"" + field + "\":{\"bind\":\"" + source + "\",\"path\":\"stats.mana\",\"fallback\":0}}").getAsJsonObject();
    }

    @Test
    public void revisionedProviderReadsOnceUntilItsEpochAdvances() {
        AtomicLong revision = new AtomicLong();
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger mana = new AtomicInteger(20);
        DataValueRegistry.registerRevisioned(id("mana"), context -> revision.get(), context -> {
            reads.incrementAndGet();
            return JsonParser.parseString("{\"stats\":{\"mana\":" + mana.get() + "}}");
        });
        BoundNumber binding = BoundNumber.parse(json("revision_test:mana", "value"), "value", 0);
        WidgetContext context = new WidgetContext(null, null, null, null);
        assertEquals(20, binding.get(context));
        assertEquals(20, binding.get(context));
        assertEquals(1, reads.get());

        mana.set(35);
        assertEquals(20, binding.get(context));
        assertEquals(1, reads.get());

        revision.incrementAndGet();
        assertEquals(35, binding.get(context));
        assertEquals(35, binding.get(context));
        assertEquals(2, reads.get());
    }

    @Test
    public void nonRevisionedProvidersRemainLive() {
        AtomicInteger reads = new AtomicInteger();
        DataValueRegistry.register(id("live"), context -> {
            reads.incrementAndGet();
            return new JsonPrimitive(reads.get());
        });
        JsonObject doc = JsonParser.parseString("{\"value\":{\"bind\":\"revision_test:live\",\"fallback\":0}}").getAsJsonObject();
        BoundNumber binding = BoundNumber.parse(doc, "value", 0);
        assertEquals(1, binding.get(null));
        assertEquals(2, binding.get(null));
        assertEquals(2, reads.get());
    }

    @Test
    public void changedWidgetContextAlwaysRefreshesSnapshot() {
        AtomicInteger reads = new AtomicInteger();
        DataValueRegistry.registerRevisioned(id("context"), context -> 0L, context -> new JsonPrimitive(reads.incrementAndGet()));
        JsonObject doc = JsonParser.parseString("{\"value\":{\"bind\":\"revision_test:context\",\"fallback\":0}}").getAsJsonObject();
        BoundNumber binding = BoundNumber.parse(doc, "value", 0);
        WidgetContext first = new WidgetContext(null, null, null, null);
        WidgetContext second = new WidgetContext(null, null, null, null);
        assertEquals(1, binding.get(first));
        assertEquals(1, binding.get(first));
        assertEquals(2, binding.get(second));
        assertEquals(2, reads.get());
    }

    @Test
    public void cachedValuesAreCopiedBeforeReturningOrFormatting() {
        AtomicInteger reads = new AtomicInteger();
        DataValueRegistry.registerRevisioned(id("object"), context -> 7L, context -> {
            reads.incrementAndGet();
            return JsonParser.parseString("{\"name\":\"Arthur\"}");
        });
        JsonObject doc = JsonParser.parseString("{\"data\":{\"bind\":\"revision_test:object\",\"fallback\":{}}}").getAsJsonObject();
        BoundData binding = BoundData.parse(doc, "data", new JsonObject(), DataBindingSpec.Kind.OBJECT);
        JsonObject first = binding.get(null).getAsJsonObject();
        first.addProperty("name", "Mutated");
        assertEquals("Arthur", binding.get(null).getAsJsonObject().get("name").getAsString());
        assertEquals(1, reads.get());
    }

    @Test
    public void failedReadsFallbackAndAreRetriedAtTheSameRevision() {
        AtomicBoolean fail = new AtomicBoolean(true);
        AtomicInteger attempts = new AtomicInteger();
        DataValueRegistry.registerRevisioned(id("flaky"), context -> 1L, context -> {
            attempts.incrementAndGet();
            if (fail.get()) {
                throw new IllegalStateException("Simulated data source failure");
            }
            return new JsonPrimitive(99);
        });
        JsonObject doc = JsonParser.parseString("{\"value\":{\"bind\":\"revision_test:flaky\",\"fallback\":-1}}").getAsJsonObject();
        BoundNumber binding = BoundNumber.parse(doc, "value", 0);
        assertEquals(-1, binding.get(null));
        fail.set(false);
        assertEquals(99, binding.get(null));
        assertEquals(99, binding.get(null));
        assertEquals(2, attempts.get());
    }

    @Test
    public void concurrentUpdatesDuringReadDoNotCommitAnOutdatedRevision() {
        AtomicInteger reads = new AtomicInteger();
        AtomicLong revision = new AtomicLong(0);
        DataValueRegistry.registerRevisioned(id("racing"), context -> revision.get(), context -> {
            int read = reads.incrementAndGet();
            if (read == 1) {
                revision.incrementAndGet();
            }
            return new JsonPrimitive(read);
        });
        JsonObject doc = JsonParser.parseString("{\"value\":{\"bind\":\"revision_test:racing\",\"fallback\":0}}").getAsJsonObject();
        BoundNumber binding = BoundNumber.parse(doc, "value", 0);
        assertEquals(1, binding.get(null));
        assertEquals(2, binding.get(null));
        assertEquals(2, binding.get(null));
        assertEquals(2, reads.get());
    }

    @Test
    public void mutableValuesAreRevisionedAndOnlyIncrementWhenPublishedContentChanges() {
        DataValueRegistry.MutableValue source = DataValueRegistry.registerMutable(id("published"), JsonParser.parseString("{\"mana\":1}"));
        DataValueRegistry.Provider provider = DataValueRegistry.find(id("published")).orElseThrow();
        assertTrue(provider instanceof DataValueRegistry.RevisionedProvider);
        DataValueRegistry.RevisionedProvider revisioned = (DataValueRegistry.RevisionedProvider) provider;
        assertEquals(0L, revisioned.revision(null));

        JsonObject published = JsonParser.parseString("{\"mana\":2}").getAsJsonObject();
        source.set(published);
        published.addProperty("mana", 500);
        assertEquals(1L, revisioned.revision(null));
        assertEquals(2, source.read(null).getAsJsonObject().get("mana").getAsInt());
        source.set(JsonParser.parseString("{\"mana\":2}"));
        assertEquals(1L, revisioned.revision(null));
        source.set(JsonNull.INSTANCE);
        assertEquals(2L, revisioned.revision(null));

        JsonObject bindingJson = JsonParser.parseString("{\"data\":{\"bind\":\"revision_test:published\",\"fallback\":{}}}").getAsJsonObject();
        BoundData binding = BoundData.parse(bindingJson, "data", new JsonObject(), DataBindingSpec.Kind.OBJECT);
        assertTrue(binding.get(null).isJsonObject());
        source.set(JsonParser.parseString("{\"mana\":4}"));
        assertEquals(4, binding.get(null).getAsJsonObject().get("mana").getAsInt());
    }

    @Test
    public void duplicateRevisionedRegistrationIsRejected() {
        DataValueRegistry.registerRevisioned(id("duplicate"), context -> 1L, context -> new JsonPrimitive(10));
        assertThrows(IllegalStateException.class, () -> DataValueRegistry.registerRevisioned(id("duplicate"), context -> 2L, context -> new JsonPrimitive(20)));
    }
}
