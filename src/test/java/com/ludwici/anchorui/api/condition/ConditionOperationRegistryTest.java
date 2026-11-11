package com.ludwici.anchorui.api.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.binding.DataValuePaths;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConditionOperationRegistryTest {
    private static ConditionExpression compile(String json) {
        return ConditionExpression.parse(JsonParser.parseString(json), false);
    }

    private static ConditionExpression.Resolver sources(Map<String, JsonElement> values) {
        return (source, path) -> DataValuePaths.resolve(values.getOrDefault(source, JsonNull.INSTANCE), path);
    }

    @Test
    public void registeredBinaryOperatorUsesResolvedValuesInNestedConditions() {
        ConditionOperationRegistry.register("mymod:even_and_greater",
                (left, right) -> left.isJsonPrimitive() && right.isJsonPrimitive()
                        && left.getAsInt() % 2 == 0 && left.getAsInt() > right.getAsInt());

        ConditionExpression expression = compile("""
                {
                  "all": [
                    {
                      "left": {"bind": "mymod:player", "path": "level"},
                      "op": "mymod:even_and_greater",
                      "right": 10
                    },
                    {"not": {"left": 0, "op": "eq", "right": 1}}
                  ]
                }
                """);

        assertEquals(1, expression.sources().size());
        assertTrue(expression.evaluate(sources(Map.of("mymod:player", JsonParser.parseString("{\"level\":12}")))));
        assertFalse(expression.evaluate(sources(Map.of("mymod:player", JsonParser.parseString("{\"level\":11}")))));
        assertFalse(expression.evaluate(sources(Map.of("mymod:player", JsonParser.parseString("{\"level\":8}")))));
    }

    @Test
    public void unaryOperationsOnlyRequireLeftOperand() {
        ConditionOperationRegistry.registerUnary("mymod:is_pokemon",
                value -> value.isJsonPrimitive() && value.getAsString().startsWith("pokemon:"));

        assertTrue(compile("""
                {"left":"pokemon:miraidon","op":"mymod:is_pokemon"}
                """).evaluate(sources(Map.of())));
        assertFalse(compile("""
                {"left":"minecraft:stone","op":"mymod:is_pokemon"}
                """).evaluate(sources(Map.of())));
        assertThrows(IllegalArgumentException.class, () -> compile("""
                {"left":"pokemon:miraidon","op":"mymod:is_pokemon","right":true}
                """));
    }

    @Test
    public void missingDataIsNotPassedToCustomOperatorUnlessFallbackIsSupplied() {
        AtomicInteger calls = new AtomicInteger();
        ConditionOperationRegistry.register("mymod:equals_fallback", (left, right) -> {
            calls.incrementAndGet();
            return left.equals(right);
        });

        assertFalse(compile("""
                {"left":{"bind":"mymod:missing"},"op":"mymod:equals_fallback","right":"unknown"}
                """).evaluate(sources(Map.of())));
        assertEquals(0, calls.get());

        assertTrue(compile("""
                {"left":{"bind":"mymod:missing","fallback":"unknown"},"op":"mymod:equals_fallback","right":"unknown"}
                """).evaluate(sources(Map.of())));
        assertEquals(1, calls.get());

        assertFalse(compile("""
                {"left":"known","op":"mymod:equals_fallback","right":{"bind":"mymod:missing"}}
                """).evaluate(sources(Map.of())));
        assertEquals(1, calls.get());
    }

    @Test
    public void validatesIdentifiersDuplicateRegistrationAndOperatorArity() {
        ConditionOperationRegistry.register("mymod:unit_binary", (left, right) -> true);

        assertTrue(ConditionOperationRegistry.find("mymod:unit_binary").isPresent());
        assertThrows(IllegalStateException.class,
                () -> ConditionOperationRegistry.register("mymod:unit_binary", (left, right) -> false));
        assertThrows(IllegalArgumentException.class,
                () -> ConditionOperationRegistry.register("not_namespaced", (left, right) -> true));
        assertThrows(IllegalArgumentException.class,
                () -> ConditionOperationRegistry.register("mymod:Invalid Uppercase", (left, right) -> true));
        assertThrows(NullPointerException.class,
                () -> ConditionOperationRegistry.register("mymod:null_predicate", null));

        assertThrows(IllegalArgumentException.class, () -> compile("""
                {"left":1,"op":"mymod:not_registered","right":2}
                """));
        assertThrows(IllegalArgumentException.class, () -> compile("""
                {"left":1,"op":"mymod:unit_binary"}
                """));
        assertThrows(IllegalArgumentException.class, () -> compile("""
                {"left":1,"op":"eq"}
                """));
    }

    @Test
    public void compiledOperationDoesNotDependOnLaterRegistryLookup() {
        ConditionOperationRegistry.register("mymod:unit_once", (left, right) -> left.equals(right));
        ConditionExpression condition = compile("""
                {"left":true,"op":"mymod:unit_once","right":true}
                """);
        assertTrue(condition.evaluate(sources(Map.of())));
        assertTrue(condition.evaluate(sources(Map.of())));
    }
}
