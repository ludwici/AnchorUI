package com.ludwici.anchorui.api.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiControlOptionsTest {
    private static UiLocalState state(String raw) {
        return new UiLocalState(JsonParser.parseString(raw).getAsJsonObject());
    }

    private static JsonObject options(String raw) {
        JsonObject json = new JsonObject();
        json.add("options", JsonParser.parseString(raw));
        return json;
    }

    @Test
    public void acceptsStringsAndLabeledOptions() {
        UiLocalState state = state("""
                {"tab":"skills"}
                """);
        List<UiControlOptions.Choice> choices = UiControlOptions.parse(options("""
                [
                  "skills",
                  {"value":"inventory","label":"Inventory"},
                  {"value":"quests","label":"Quest journal"}
                ]
                """), state, "tab");
        assertEquals(3, choices.size());
        assertEquals("skills", choices.get(0).label());
        assertEquals("Inventory", choices.get(1).label());
        assertEquals("quests", choices.get(2).value().getAsString());
        assertEquals(0, UiControlOptions.selectedIndex(choices, state.get("tab")));
        state.set("tab", new JsonPrimitive("inventory"));
        assertEquals(1, UiControlOptions.selectedIndex(choices, state.get("tab")));
        assertEquals(-1, UiControlOptions.selectedIndex(choices, new JsonPrimitive("not_found")));
    }

    @Test
    public void acceptsNumericChoicesAndPreservesTypes() {
        UiLocalState state = state("""
                {"page":1}
                """);
        List<UiControlOptions.Choice> choices = UiControlOptions.parse(options("""
                [
                  {"value":1,"label":"Page one"},
                  {"value":2,"label":"Page two"}
                ]
                """), state, "page");
        assertEquals(0, UiControlOptions.selectedIndex(choices, new JsonPrimitive(1)));
        assertEquals(2, choices.get(1).value().getAsInt());
        assertEquals("Page two", choices.get(1).label());
        assertFalse(choices.get(0).value().isJsonObject());
    }

    @Test
    public void rejectsMalformedOptionsAndUnknownState() {
        UiLocalState state = state("""
                {"tab":"skills","enabled":true}
                """);
        for (String raw : List.of(
                "[]",
                "[1,1]",
                "[\"skills\",\"skills\"]",
                "[true]",
                "[null]",
                "[{}]",
                "[{\"value\":\"skills\",\"label\":\"\"}]",
                "[{\"value\":{\"id\":1}}]",
                "[{\"value\":\"inventory\"}]",
                "[{\"value\":1}]"
        )) {
            assertThrows(IllegalArgumentException.class, () ->
                    UiControlOptions.parse(options(raw), state, "tab"), raw);
        }
        assertThrows(IllegalArgumentException.class, () ->
                UiControlOptions.parse(options("[\"skills\"]"), state, "missing"));
        assertThrows(IllegalArgumentException.class, () ->
                UiControlOptions.parse(options("[\"skills\"]"), state, "enabled"));
        assertThrows(IllegalArgumentException.class, () ->
                UiControlOptions.parse(new JsonObject(), state, "tab"));

        JsonArray tooMany = new JsonArray();
        for (int index = 0; index < 65; index++) {
            tooMany.add("option_" + index);
        }
        JsonObject config = new JsonObject();
        config.add("options", tooMany);
        assertThrows(IllegalArgumentException.class, () -> UiControlOptions.parse(config, state, "tab"));
    }

    @Test
    public void returnedValuesCannotMutateChoice() {
        UiLocalState state = state("""
                {"tab":"skills"}
                """);
        UiControlOptions.Choice choice = UiControlOptions.parse(options("[\"skills\"]"), state, "tab").getFirst();
        assertEquals("skills", choice.value().getAsString());
        assertTrue(choice.value() instanceof JsonPrimitive);
    }
}
