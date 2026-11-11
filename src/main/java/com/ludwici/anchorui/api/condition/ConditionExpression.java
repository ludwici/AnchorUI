package com.ludwici.anchorui.api.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.binding.DataValuePaths;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ConditionExpression {
    @FunctionalInterface
    public interface Resolver {
        JsonElement read(String source, List<String> path);
    }

    private interface Test {
        boolean evaluate(Resolver resolver);
    }

    private interface Operand {
        Value resolve(Resolver resolver);
    }

    private record Value(JsonElement element, boolean present, boolean valid) {
    }

    private enum Operator {
        EQ, NE, GT, GTE, LT, LTE, CONTAINS, STARTS_WITH, ENDS_WITH, IN, NOT_IN, EXISTS, IS_NULL, EMPTY;

        private static Operator builtin(String input) {
            return switch (input) {
                case "eq" -> EQ;
                case "ne" -> NE;
                case "gt" -> GT;
                case "gte" -> GTE;
                case "lt" -> LT;
                case "lte" -> LTE;
                case "contains" -> CONTAINS;
                case "startsWith" -> STARTS_WITH;
                case "endsWith" -> ENDS_WITH;
                case "in" -> IN;
                case "notIn" -> NOT_IN;
                case "exists" -> EXISTS;
                case "isNull" -> IS_NULL;
                case "empty" -> EMPTY;
                default -> null;
            };
        }

        private boolean unary() {
            return this == EXISTS || this == IS_NULL || this == EMPTY;
        }
    }

    private static final class Parser {
        private final Set<String> sources = new LinkedHashSet<>();
        private int nodes;

        private Test condition(JsonElement json, boolean defaultValue, boolean missingBindingDefault, int depth) {
            limit(depth);
            if (json == null || json.isJsonNull()) {
                return resolver -> defaultValue;
            }
            if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) {
                boolean value = json.getAsBoolean();
                return resolver -> value;
            }
            if (!json.isJsonObject()) {
                throw new IllegalArgumentException("Condition must be a boolean or a condition object");
            }
            JsonObject object = json.getAsJsonObject();
            if (object.has("bind")) {
                requireKeys(object, "bind", "path", "fallback");
                Operand operand = operand(json, depth + 1);
                JsonElement fallback = object.has("fallback") ? object.get("fallback") : new JsonPrimitive(missingBindingDefault);
                if (!booleanValue(fallback)) {
                    throw new IllegalArgumentException("Boolean condition fallback must be true or false");
                }
                boolean fallbackValue = fallback.getAsBoolean();
                return resolver -> {
                    Value value = operand.resolve(resolver);
                    return value.valid() && booleanValue(value.element())
                            ? value.element().getAsBoolean() : fallbackValue;
                };
            }
            int logicCount = (object.has("all") ? 1 : 0) + (object.has("any") ? 1 : 0)
                    + (object.has("not") ? 1 : 0);
            if (logicCount > 0) {
                if (logicCount != 1 || object.size() != 1) {
                    throw new IllegalArgumentException("Condition logic must contain exactly one of all, any or not");
                }
                if (object.has("not")) {
                    Test child = condition(object.get("not"), false, false, depth + 1);
                    return resolver -> !child.evaluate(resolver);
                }
                String key = object.has("all") ? "all" : "any";
                JsonElement raw = object.get(key);
                if (raw == null || !raw.isJsonArray()) {
                    throw new IllegalArgumentException(key + " must be an array");
                }
                JsonArray array = raw.getAsJsonArray();
                if (array.isEmpty() || array.size() > 32) {
                    throw new IllegalArgumentException(key + " must contain between 1 and 32 conditions");
                }
                List<Test> children = new ArrayList<>(array.size());
                for (JsonElement entry : array) {
                    children.add(condition(entry, false, false, depth + 1));
                }
                List<Test> compiled = List.copyOf(children);
                if ("all".equals(key)) {
                    return resolver -> {
                        for (Test child : compiled) {
                            if (!child.evaluate(resolver)) {
                                return false;
                            }
                        }
                        return true;
                    };
                }
                return resolver -> {
                    for (Test child : compiled) {
                        if (child.evaluate(resolver)) {
                            return true;
                        }
                    }
                    return false;
                };
            }
            requireKeys(object, "left", "op", "right");
            JsonElement operatorValue = object.get("op");
            if (operatorValue == null || !operatorValue.isJsonPrimitive()
                    || !operatorValue.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Condition comparison requires a string 'op'");
            }
            String operatorId = operatorValue.getAsString();
            Operator builtin = Operator.builtin(operatorId);
            ConditionOperationRegistry.Definition custom = null;
            if (builtin == null) {
                custom = ConditionOperationRegistry.find(operatorId)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown condition operator: " + operatorId
                                + " (register a namespaced operation before loading the UI)"));
            }
            boolean unary = builtin != null ? builtin.unary() : custom.unary();
            if (!object.has("left") || (!unary && !object.has("right"))
                    || (unary && object.has("right"))) {
                throw new IllegalArgumentException("Invalid operands for condition operator: " + operatorId);
            }
            Operand left = operand(object.get("left"), depth + 1);
            Operand right = unary ? null : operand(object.get("right"), depth + 1);
            if (custom != null) {
                ConditionOperationRegistry.Definition operation = custom;
                return resolver -> {
                    Value first = left.resolve(resolver);
                    if (!first.valid() || first.element().isJsonNull()) {
                        return false;
                    }
                    Value second = right == null ? null : right.resolve(resolver);
                    if (second != null && (!second.valid() || second.element().isJsonNull())) {
                        return false;
                    }
                    return operation.test(first.element(), second == null ? null : second.element());
                };
            }
            Operator operation = builtin;
            return resolver -> compare(operation, left.resolve(resolver),
                    right == null ? null : right.resolve(resolver));
        }

        private Operand operand(JsonElement json, int depth) {
            limit(depth);
            if (json == null) {
                throw new IllegalArgumentException("Condition operand must not be absent");
            }
            if (!json.isJsonObject() || !json.getAsJsonObject().has("bind")) {
                JsonElement literal = json.deepCopy();
                return resolver -> new Value(literal, !literal.isJsonNull(), true);
            }
            JsonObject object = json.getAsJsonObject();
            requireKeys(object, "bind", "path", "fallback");
            JsonElement sourceElement = object.get("bind");
            if (sourceElement == null || !sourceElement.isJsonPrimitive()
                    || !sourceElement.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Condition binding requires a namespaced string 'bind'");
            }
            String source = sourceElement.getAsString();
            if (!source.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("Invalid namespaced condition source: " + source);
            }
            String path = "";
            if (object.has("path")) {
                JsonElement rawPath = object.get("path");
                if (!rawPath.isJsonPrimitive() || !rawPath.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("Condition binding path must be a string");
                }
                path = rawPath.getAsString();
            }
            List<String> tokens = DataValuePaths.tokens(path);
            sources.add(source);
            boolean hasFallback = object.has("fallback");
            JsonElement fallback = hasFallback ? object.get("fallback").deepCopy() : JsonNull.INSTANCE;
            return resolver -> {
                JsonElement raw;
                try {
                    raw = resolver.read(source, tokens);
                } catch (RuntimeException exception) {
                    raw = JsonNull.INSTANCE;
                }
                if (raw != null && !raw.isJsonNull()) {
                    return new Value(raw, true, true);
                }
                return new Value(fallback, false, hasFallback);
            };
        }

        private void limit(int depth) {
            if (depth > 20 || ++nodes > 256) {
                throw new IllegalArgumentException("Condition exceeds nesting or node limit");
            }
        }
    }

    private final Test expression;
    private final Set<String> sources;

    private ConditionExpression(Test expression, Set<String> sources) {
        this.expression = expression;
        this.sources = sources;
    }

    public static ConditionExpression parse(JsonElement json, boolean defaultValue) {
        return parse(json, defaultValue, defaultValue);
    }

    public static ConditionExpression parse(JsonElement json, boolean defaultValue, boolean missingBindingDefault) {
        Parser parser = new Parser();
        Test expression = parser.condition(json, defaultValue, missingBindingDefault, 0);
        return new ConditionExpression(expression, Set.copyOf(parser.sources));
    }

    public boolean evaluate(Resolver resolver) {
        return expression.evaluate(resolver);
    }

    public Set<String> sources() {
        return sources;
    }

    private static boolean compare(Operator operator, Value left, Value right) {
        if (operator == Operator.EXISTS) {
            return left.present();
        }
        if (operator == Operator.IS_NULL) {
            return !left.present() || left.element().isJsonNull();
        }
        if (!left.valid() || left.element().isJsonNull()) {
            return false;
        }
        if (operator == Operator.EMPTY) {
            return empty(left.element());
        }
        if (right == null || !right.valid() || right.element().isJsonNull()) {
            return false;
        }
        JsonElement a = left.element();
        JsonElement b = right.element();
        return switch (operator) {
            case EQ -> equal(a, b);
            case NE -> !equal(a, b);
            case GT -> ordered(a, b, 1);
            case GTE -> ordered(a, b, 2);
            case LT -> ordered(a, b, -1);
            case LTE -> ordered(a, b, -2);
            case CONTAINS -> contains(a, b);
            case STARTS_WITH -> strings(a, b) && a.getAsString().startsWith(b.getAsString());
            case ENDS_WITH -> strings(a, b) && a.getAsString().endsWith(b.getAsString());
            case IN -> b.isJsonArray() && contains(b, a);
            case NOT_IN -> b.isJsonArray() && !contains(b, a);
            default -> false;
        };
    }

    private static boolean ordered(JsonElement left, JsonElement right, int variant) {
        if (!number(left) || !number(right)) {
            return false;
        }
        double a = left.getAsDouble();
        double b = right.getAsDouble();
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            return false;
        }
        return switch (variant) {
            case 1 -> a > b;
            case 2 -> a >= b;
            case -1 -> a < b;
            case -2 -> a <= b;
            default -> false;
        };
    }

    private static boolean equal(JsonElement a, JsonElement b) {
        if (number(a) || number(b)) {
            return number(a) && number(b) && Double.isFinite(a.getAsDouble())
                    && Double.isFinite(b.getAsDouble()) && a.getAsDouble() == b.getAsDouble();
        }
        return a.equals(b);
    }

    private static boolean contains(JsonElement a, JsonElement b) {
        if (a.isJsonArray()) {
            for (JsonElement item : a.getAsJsonArray()) {
                if (equal(item, b)) {
                    return true;
                }
            }
            return false;
        }
        return strings(a, b) && a.getAsString().contains(b.getAsString());
    }

    private static boolean empty(JsonElement element) {
        if (element.isJsonArray()) {
            return element.getAsJsonArray().isEmpty();
        }
        if (element.isJsonObject()) {
            return element.getAsJsonObject().size() == 0;
        }
        return stringValue(element) && element.getAsString().isEmpty();
    }

    private static boolean strings(JsonElement a, JsonElement b) {
        return stringValue(a) && stringValue(b);
    }

    private static boolean stringValue(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    private static boolean booleanValue(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean();
    }

    private static boolean number(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
    }

    private static void requireKeys(JsonObject object, String... valid) {
        for (String key : object.keySet()) {
            boolean accepted = false;
            for (String allowed : valid) {
                if (key.equals(allowed)) {
                    accepted = true;
                    break;
                }
            }
            if (!accepted) {
                throw new IllegalArgumentException("Unexpected condition field: " + key);
            }
        }
    }
}
