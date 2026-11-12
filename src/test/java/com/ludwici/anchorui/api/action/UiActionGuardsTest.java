package com.ludwici.anchorui.api.action;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.binding.DataValueRegistry;
import com.ludwici.anchorui.api.widget.TemplateListExpander;
import java.util.List;
import java.util.Map;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiActionGuardsTest {
    private record Arguments(int amount) {
        private static final Codec<Arguments> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("amount").forGetter(Arguments::amount)
        ).apply(instance, Arguments::new));
    }

    private static JsonObject object(String source) {
        return JsonParser.parseString(source).getAsJsonObject();
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath("action_guard_tests", name);
    }

    @Test
    public void guardEvaluatesLatestDataAtClickTimeWithoutRecompilation() {
        DataValueRegistry.MutableValue enabled = DataValueRegistry.registerMutable(id("enabled"), new JsonPrimitive(false));
        AtomicInteger fired = new AtomicInteger();
        UiActionRegistry.registerEvent(id("notice"), (context, arguments) -> {
            assertFalse(arguments.has("when"));
            return event -> {
                assertEquals("onClick", event.name());
                fired.incrementAndGet();
            };
        });
        UiEventBindings bindings = UiEventBindings.compile(null, object("""
                {
                  "onClick": {
                    "type": "action_guard_tests:notice",
                    "when": {"bind": "action_guard_tests:enabled", "fallback": false}
                  }
                }
                """), "onClick");

        bindings.fire("onClick", "example:button", new JsonPrimitive(1));
        assertEquals(0, fired.get());
        enabled.set(new JsonPrimitive(true));
        bindings.fire("onClick", "example:button", new JsonPrimitive(2));
        assertEquals(1, fired.get());
        enabled.set(new JsonPrimitive(false));
        bindings.fire("onClick", "example:button", new JsonPrimitive(3));
        assertEquals(1, fired.get());
    }

    @Test
    public void typedActionsAcceptGuardsWithoutChangingTheirArguments() {
        AtomicInteger total = new AtomicInteger();
        UiActionRegistry.registerEvent(id("typed"), Arguments.CODEC,
                (context, arguments) -> event -> total.addAndGet(arguments.amount()));

        UiActionRegistry.compile(null, object("""
                {"type":"action_guard_tests:typed","amount":4,"when":false}
                """)).run();
        assertEquals(0, total.get());
        UiActionRegistry.compile(null, object("""
                {"type":"action_guard_tests:typed","amount":4,"when":true}
                """)).run();
        assertEquals(4, total.get());
    }

    @Test
    public void malformedAndMissingGuardDataAreRejectedOrFailClosed() {
        UiActionRegistry.register(id("noop"), (context, arguments) -> () -> {});

        assertThrows(IllegalArgumentException.class, () -> UiActionRegistry.compile(null, object("""
                {"type":"action_guard_tests:noop","when":{"op":"unknown"}}
                """)));
        assertThrows(IllegalArgumentException.class, () -> UiActionRegistry.compile(null, object("""
                {"type":"action_guard_tests:noop","when":{"bind":"action_guard_tests:not_registered"}}
                """)));

        AtomicInteger attempts = new AtomicInteger();
        UiActionRegistry.register(id("counter"), (context, arguments) -> attempts::incrementAndGet);
        UiActionRegistry.compile(null, object("""
                {"type":"action_guard_tests:counter","when":false}
                """)).run();
        assertEquals(0, attempts.get());
        assertTrue(UiActionRegistry.compileEvent(null, object("""
                {"type":"action_guard_tests:counter","when":true}
                """)) != null);
    }
    @Test
    public void repeatedRowActionsEvaluateSubstitutedItemConditions() {
        AtomicInteger count = new AtomicInteger();
        UiActionRegistry.register(id("repeated"), (context, arguments) -> count::incrementAndGet);
        JsonObject list = object("""
                {
                  "key": "id",
                  "template": {
                    "type": "anchorui:button",
                    "onClick": {
                      "type": "action_guard_tests:repeated",
                      "when": {
                        "left": {"item": "level"},
                        "op": "gte",
                        "right": 10
                      }
                    }
                  }
                }
                """);
        List<TemplateListExpander.Entry> rows = TemplateListExpander.expand(list,
                JsonParser.parseString("""
                        [{"id":"starter","level":3},{"id":"expert","level":15}]
                        """).getAsJsonArray(), "action_guard_tests:rows", Map.of());
        for (TemplateListExpander.Entry row : rows) {
            UiActionRegistry.compile(null, row.settings().get("onClick")).run();
        }
        assertEquals(1, count.get());
    }

}
