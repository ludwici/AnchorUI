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

public class ConditionExpressionTest {
    private static ConditionExpression compile(String json) {
        return ConditionExpression.parse(JsonParser.parseString(json), false);
    }

    private static ConditionExpression.Resolver sources(Map<String, JsonElement> values) {
        return (id, path) -> DataValuePaths.resolve(values.getOrDefault(id, JsonNull.INSTANCE), path);
    }

    @Test
    public void acceptsLegacyBooleanLiteralsAndDefaults() {
        assertTrue(ConditionExpression.parse(null, true).evaluate(sources(Map.of())));
        assertFalse(ConditionExpression.parse(null, false).evaluate(sources(Map.of())));
        assertTrue(compile("true").evaluate(sources(Map.of())));
        assertFalse(compile("false").evaluate(sources(Map.of())));
        assertThrows(IllegalArgumentException.class, () -> compile("\"true\""));
        assertThrows(IllegalArgumentException.class, () -> compile("14"));
    }

    @Test
    public void supportsNestedAnyAllNotAndStructuredSources() {
        var expression = compile("""
                {
                  "all": [
                    {"left":{"bind":"demo:player","path":"level","fallback":0},"op":"gte","right":10},
                    {"any":[
                      {"left":{"bind":"demo:player","path":"role"},"op":"eq","right":"medic"},
                      {"not":{"bind":"demo:creative","fallback":false}}
                    ]}
                  ]
                }
                """);
        var state = sources(Map.of("demo:player", JsonParser.parseString("{\"level\":12,\"role\":\"engineer\"}"), "demo:creative", JsonParser.parseString("false")));
        assertTrue(expression.evaluate(state));
        assertEquals(2, expression.sources().size());
        assertFalse(expression.evaluate(sources(Map.of("demo:player", JsonParser.parseString("{\"level\":8,\"role\":\"medic\"}")))));
        assertFalse(expression.evaluate(sources(Map.of("demo:player", JsonParser.parseString("{\"level\":12,\"role\":\"engineer\"}"), "demo:creative", JsonParser.parseString("true")))));
    }

    @Test
    public void shortCircuitsAllAndAny() {
        AtomicInteger reads = new AtomicInteger();
        ConditionExpression.Resolver source = (id, path) -> {
            reads.incrementAndGet();
            return JsonParser.parseString("true");
        };
        var all = compile("""
                {"all":[false,{"bind":"demo:unneeded","fallback":false}]}
                """);
        assertFalse(all.evaluate(source));
        assertEquals(0, reads.get());

        var any = compile("""
                {"any":[true,{"bind":"demo:unneeded","fallback":false}]}
                """);
        assertTrue(any.evaluate(source));
        assertEquals(0, reads.get());
    }

    @Test
    public void comparesNumbersStrictlyWithoutStringCoercion() {
        assertTrue(compile("""
                {"left":10,"op":"eq","right":10.0}
                """).evaluate(sources(Map.of())));
        assertFalse(compile("""
                {"left":10,"op":"eq","right":"10"}
                """).evaluate(sources(Map.of())));
        assertFalse(compile("""
                {"left":"10","op":"gte","right":5}
                """).evaluate(sources(Map.of())));
        assertTrue(compile("""
                {"all":[
                  {"left":8,"op":"gt","right":7},
                  {"left":8,"op":"gte","right":8},
                  {"left":7,"op":"lt","right":8},
                  {"left":8,"op":"lte","right":8},
                  {"left":8,"op":"ne","right":7}
                ]}
                """).evaluate(sources(Map.of())));
    }

    @Test
    public void supportsStringsAndArrays() {
        assertTrue(compile("""
                {"all":[
                  {"left":"Miraidon","op":"contains","right":"raid"},
                  {"left":"Miraidon","op":"startsWith","right":"Mir"},
                  {"left":"Miraidon","op":"endsWith","right":"don"},
                  {"left":["medic","engineer"],"op":"contains","right":"medic"},
                  {"left":"medic","op":"in","right":["trainer","medic"]},
                  {"left":"engineer","op":"notIn","right":["trainer","medic"]},
                  {"left":[],"op":"empty"},
                  {"left":"","op":"empty"}
                ]}
                """).evaluate(sources(Map.of())));
        assertFalse(compile("""
                {"left":123,"op":"contains","right":"2"}
                """).evaluate(sources(Map.of())));
    }

    @Test
    public void respectsFallbackButExistsChecksOriginalSource() {
        ConditionExpression.Resolver missing = sources(Map.of());
        assertTrue(compile("""
                {"left":{"bind":"demo:missing","path":"status","fallback":"locked"},"op":"eq","right":"locked"}
                """).evaluate(missing));
        assertFalse(compile("""
                {"left":{"bind":"demo:missing"},"op":"ne","right":"locked"}
                """).evaluate(missing));
        assertFalse(compile("""
                {"left":{"bind":"demo:missing","fallback":true},"op":"exists"}
                """).evaluate(missing));
        assertTrue(compile("""
                {"left":{"bind":"demo:missing"},"op":"isNull"}
                """).evaluate(missing));
        assertTrue(compile("""
                {"left":{"bind":"demo:present","path":"quest"},"op":"exists"}
                """).evaluate(sources(Map.of("demo:present", JsonParser.parseString("{\"quest\":\"active\"}")))));
    }

    @Test
    public void handlesLegacyBooleanBindingsAndFailClosedEnabledFallback() {
        JsonElement bound = JsonParser.parseString("""
                {"bind":"demo:can_use"}
                """);
        var resolver = sources(Map.of("demo:can_use", JsonParser.parseString("true")));
        assertTrue(ConditionExpression.parse(bound, true).evaluate(resolver));
        assertTrue(ConditionExpression.parse(bound, true).evaluate(sources(Map.of())));
        assertFalse(ConditionExpression.parse(bound, true, false).evaluate(sources(Map.of())));
        assertFalse(ConditionExpression.parse(bound, true, false).evaluate(sources(Map.of("demo:can_use", JsonParser.parseString("\"wrong type\"")))));
        assertTrue(ConditionExpression.parse(JsonParser.parseString("""
                {"bind":"demo:can_use","fallback":true}
                """), true, false).evaluate(sources(Map.of())));
        assertTrue(ConditionExpression.parse(JsonParser.parseString("""
                {"bind":"demo:can_use","fallback":false}
                """), false).evaluate(resolver));
    }

    @Test
    public void rejectsInvalidFormsWithClearErrors() {
        String[] invalid = {
                "{\"all\":[]}",
                "{\"any\":true}",
                "{\"not\":false,\"all\":[true]}",
                "{\"op\":\"gte\",\"left\":1}",
                "{\"op\":\"unknown\",\"left\":1,\"right\":2}",
                "{\"op\":\"exists\",\"left\":1,\"right\":2}",
                "{\"bind\":\"NotNamespaced\"}",
                "{\"bind\":\"demo:data\",\"path\":\"a..b\"}",
                "{\"bind\":\"demo:data\",\"fallback\":\"false\"}",
                "{\"left\":true,\"op\":\"eq\",\"right\":true,\"unexpected\":5}"
        };
        for (String json : invalid) {
            assertThrows(IllegalArgumentException.class, () -> compile(json), json);
        }
    }

    @Test
    public void rejectsUnboundedNesting() {
        String nested = "true";
        for (int index = 0; index < 26; index++) {
            nested = "{\"not\":" + nested + "}";
        }
        String input = nested;
        assertThrows(IllegalArgumentException.class, () -> compile(input));
    }

    @Test
    public void missingOrIncorrectValueDoesNotAccidentallyPassComparison() {
        var condition = compile("""
                {"left":{"bind":"demo:profile","path":"stats.mana"},"op":"gt","right":10}
                """);
        assertFalse(condition.evaluate(sources(Map.of("demo:profile", JsonParser.parseString("{\"stats\":{\"mana\":\"not a number\"}}")))));
        assertFalse(condition.evaluate(sources(Map.of("demo:profile", JsonParser.parseString("{\"stats\":{}}")))));
        assertTrue(condition.evaluate(sources(Map.of("demo:profile", JsonParser.parseString("{\"stats\":{\"mana\":35}}")))));
    }
}
