package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataBindingSpecTest {
    @Test
    public void parsesAllSupportedLiteralTypes() {
        assertFalse(DataBindingSpec.parse(new JsonPrimitive("Hello"), new JsonPrimitive("?"), DataBindingSpec.Kind.STRING).isBound());
        assertFalse(DataBindingSpec.parse(new JsonPrimitive(true), new JsonPrimitive(false), DataBindingSpec.Kind.BOOLEAN).isBound());
        assertFalse(DataBindingSpec.parse(new JsonArray(), new JsonArray(), DataBindingSpec.Kind.LIST).isBound());
        assertFalse(DataBindingSpec.parse(new JsonObject(), new JsonObject(), DataBindingSpec.Kind.OBJECT).isBound());
        assertFalse(DataBindingSpec.parse(new JsonPrimitive(24), new JsonPrimitive(0), DataBindingSpec.Kind.NUMBER).isBound());
    }

    @Test
    public void parsesObjectAndListBindingsWithPathsAndFallbacks() {
        var number = DataBindingSpec.parse(JsonParser.parseString("""
                {"bind":"anchorui:player.profile","path":"stats.health","fallback":20}
                """), new JsonPrimitive(0), DataBindingSpec.Kind.NUMBER);
        assertEquals("anchorui:player.profile", number.source());
        assertEquals("stats.health", number.path());
        assertEquals(20, number.fallback().getAsInt());
        assertTrue(number.isBound());

        var list = DataBindingSpec.parse(JsonParser.parseString("""
                {"bind":"mymod:quests","path":"active","fallback":[]}
                """), new JsonArray(), DataBindingSpec.Kind.LIST);
        assertEquals("mymod:quests", list.source());
        assertEquals("active", list.path());
        assertTrue(list.fallback().isJsonArray());

        var text = DataBindingSpec.parse(JsonParser.parseString("""
                {"bind":"anchorui:player.name","fallback":"Unknown"}
                """), new JsonPrimitive(""), DataBindingSpec.Kind.STRING);
        assertEquals("Unknown", text.fallback().getAsString());

        var booleanBinding = DataBindingSpec.parse(JsonParser.parseString("""
                {"bind":"anchorui:player.creative","fallback":false}
                """), new JsonPrimitive(true), DataBindingSpec.Kind.BOOLEAN);
        assertFalse(booleanBinding.fallback().getAsBoolean());
    }

    @Test
    public void rejectsWrongTypesAndInvalidBindingPaths() {
        assertThrows(IllegalArgumentException.class, () -> DataBindingSpec.parse(new JsonPrimitive("true"), new JsonPrimitive(false), DataBindingSpec.Kind.BOOLEAN));
        assertThrows(IllegalArgumentException.class, () -> DataBindingSpec.parse(new JsonPrimitive("42"), new JsonPrimitive(0), DataBindingSpec.Kind.NUMBER));
        assertThrows(IllegalArgumentException.class, () -> DataBindingSpec.parse(JsonParser.parseString("{\"bind\":\"INVALID NAME\"}"), new JsonPrimitive(""), DataBindingSpec.Kind.STRING));
        assertThrows(IllegalArgumentException.class, () -> DataBindingSpec.parse(JsonParser.parseString("{\"bind\":\"test:foo\",\"path\":\"obj..bad\"}"), new JsonPrimitive(""), DataBindingSpec.Kind.STRING));
        assertThrows(IllegalArgumentException.class, () -> DataBindingSpec.parse(JsonParser.parseString("{\"bind\":\"test:foo\",\"fallback\":\"false\"}"), new JsonPrimitive(false), DataBindingSpec.Kind.BOOLEAN));
        assertThrows(IllegalArgumentException.class, () -> DataBindingSpec.parse(JsonParser.parseString("1e999"), new JsonPrimitive(0), DataBindingSpec.Kind.NUMBER));
    }
}
