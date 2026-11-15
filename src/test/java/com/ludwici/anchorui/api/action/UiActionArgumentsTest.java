package com.ludwici.anchorui.api.action;

import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiActionArgumentsTest {
    private record QuestArguments(String quest, int amount, boolean silent) {
    }

    private static final Codec<QuestArguments> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("quest").forGetter(QuestArguments::quest),
            Codec.INT.optionalFieldOf("amount", 1).forGetter(QuestArguments::amount),
            Codec.BOOL.optionalFieldOf("silent", false).forGetter(QuestArguments::silent)
    ).apply(instance, QuestArguments::new));

    private static final Codec<QuestArguments> QUEST_CODEC = BASE_CODEC.validate(arguments -> {
        if (!arguments.quest().contains(":") || ResourceLocation.tryParse(arguments.quest()) == null) {
            return DataResult.error(() -> "quest must be a namespaced identifier");
        }
        if (arguments.amount() < 1) {
            return DataResult.error(() -> "amount must be positive");
        }
        return DataResult.success(arguments);
    });

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath("ui_action_args_test", name);
    }

    @Test
    public void shortSyntaxWorksForLegacyActionsAndEveryEventBinding() {
        AtomicInteger count = new AtomicInteger();
        UiActionRegistry.register(id("legacy"), (context, settings) -> {
            assertEquals("ui_action_args_test:legacy", settings.get("type").getAsString());
            return count::incrementAndGet;
        });

        UiActionRegistry.compile(null, new JsonPrimitive("ui_action_args_test:legacy")).run();
        UiActionRegistry.compile(null, JsonParser.parseString("""
                {
                  "type": "ui_action_args_test:legacy"
                }
                """)).run();

        UiEventBindings bindings = UiEventBindings.compile(null, JsonParser.parseString("""
                {
                  "onOpen": "ui_action_args_test:legacy",
                  "onHoverEnter": "ui_action_args_test:legacy",
                  "onClick": "ui_action_args_test:legacy"
                }
                """).getAsJsonObject(), "onOpen", "onHoverEnter", "onClick");
        for (String name : new String[]{"onOpen", "onHoverEnter", "onClick"}) {
            assertTrue(bindings.has(name));
            bindings.fire(name, "ui_action_args_test:screen", JsonNull.INSTANCE);
        }
        assertEquals(5, count.get());
    }

    @Test
    public void typedEventArgumentsUseNamedFieldsDefaultsAndMetadata() {
        AtomicReference<QuestArguments> receivedArguments = new AtomicReference<>();
        AtomicReference<UiEvent> receivedEvent = new AtomicReference<>();
        AtomicInteger factoryCalls = new AtomicInteger();
        UiActionRegistry.registerEvent(id("accept_quest"), QUEST_CODEC, (context, arguments) -> {
            factoryCalls.incrementAndGet();
            return event -> {
                receivedArguments.set(arguments);
                receivedEvent.set(event);
            };
        });

        UiEventBindings configured = UiEventBindings.compile(null, JsonParser.parseString("""
                {
                  "onClick": {
                    "type": "ui_action_args_test:accept_quest",
                    "quest": "example:first_quest",
                    "amount": 3,
                    "silent": true
                  }
                }
                """).getAsJsonObject(), "onClick");
        assertEquals(1, factoryCalls.get());
        configured.fire("onClick", "example:accept_button", JsonNull.INSTANCE);
        assertEquals(new QuestArguments("example:first_quest", 3, true), receivedArguments.get());
        assertEquals("onClick", receivedEvent.get().name());
        assertEquals("example:accept_button", receivedEvent.get().targetId());

        UiEventBindings defaults = UiEventBindings.compile(null, JsonParser.parseString("""
                {
                  "onChange": {
                    "type": "ui_action_args_test:accept_quest",
                    "quest": "example:second_quest"
                  }
                }
                """).getAsJsonObject(), "onChange");
        defaults.fire("onChange", "example:list", new JsonPrimitive(2));
        assertEquals(new QuestArguments("example:second_quest", 1, false), receivedArguments.get());
        assertEquals(2, receivedEvent.get().value().getAsInt());
        assertEquals(2, factoryCalls.get());
    }

    @Test
    public void typedRunnableRegistrationPreservesTheExistingActionApi() {
        AtomicReference<QuestArguments> received = new AtomicReference<>();
        UiActionRegistry.register(id("runnable_quest"), QUEST_CODEC, (context, arguments) -> () -> received.set(arguments));

        UiActionRegistry.compile(null, JsonParser.parseString("""
                {
                  "type": "ui_action_args_test:runnable_quest",
                  "quest": "example:another",
                  "silent": true
                }
                """)).run();
        assertEquals(new QuestArguments("example:another", 1, true), received.get());
    }

    @Test
    public void invalidOrMissingRequiredArgumentsFailDuringCompilation() {
        UiActionRegistry.registerEvent(id("strict_quest"), QUEST_CODEC, (context, arguments) -> event -> {});

        for (String raw : new String[]{
                "\"ui_action_args_test:strict_quest\"",
                "{\"type\":\"ui_action_args_test:strict_quest\"}",
                "{\"type\":\"ui_action_args_test:strict_quest\",\"quest\":17}",
                "{\"type\":\"ui_action_args_test:strict_quest\",\"quest\":\"not_namespaced\"}",
                "{\"type\":\"ui_action_args_test:strict_quest\",\"quest\":\"example:valid\",\"amount\":0}",
                "{\"type\":\"ui_action_args_test:strict_quest\",\"quest\":\"example:valid\",\"silent\":\"yes\"}"
        }) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> UiActionRegistry.compileEvent(null, JsonParser.parseString(raw)));
            assertTrue(exception.getMessage().contains("ui_action_args_test:strict_quest"));
        }
    }

    @Test
    public void invalidActionSyntaxAndDuplicateRegistrationAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> UiActionRegistry.compileEvent(null, new JsonPrimitive(42)));
        assertThrows(IllegalArgumentException.class, () -> UiActionRegistry.compileEvent(null, new JsonPrimitive("bad_id")));
        assertThrows(IllegalArgumentException.class, () -> UiActionRegistry.compileEvent(null, new JsonPrimitive("ui_action_args_test:unknown")));
        assertThrows(IllegalArgumentException.class, () -> UiActionRegistry.compileEvent(null, JsonParser.parseString("{}")));

        UiActionRegistry.register(id("duplicate"), QUEST_CODEC, (context, arguments) -> () -> {});
        assertThrows(IllegalStateException.class, () ->
                UiActionRegistry.registerEvent(id("duplicate"), (context, settings) -> event -> {}));
        assertFalse(UiEventBindings.compile(null, JsonParser.parseString("{}").getAsJsonObject(), "onOpen").has("onOpen"));
    }
}
