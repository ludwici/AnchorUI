package com.ludwici.anchorui.api.state;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiLocalStateTest {
    @Test
    public void storesDeclaredValuesWithTheirOriginalTypes() {
        var initial = JsonParser.parseString("""
                {
                  "tab": "skills",
                  "details": false,
                  "page": 1,
                  "selection": {
                    "id": "rpg:default"
                  }
                }
                """).getAsJsonObject();
        UiLocalState state = new UiLocalState(initial);
        initial.addProperty("tab", "changed");
        assertEquals("skills", state.get("tab").getAsString());
        assertEquals(1, state.get("page").getAsInt());

        state.set("tab", new JsonPrimitive("inventory"));
        state.set("page", new JsonPrimitive(2));
        assertTrue(state.toggle("details"));
        assertFalse(state.toggle("details"));
        assertEquals("inventory", state.snapshot().get("tab").getAsString());
        assertEquals(2, state.get("page").getAsInt());
        assertFalse(state.get("details").getAsBoolean());

        var extracted = state.snapshot();
        extracted.addProperty("tab", "malicious");
        assertEquals("inventory", state.get("tab").getAsString());
    }

    @Test
    public void preservesObjectsAndPreventsMutationThroughAccessors() {
        var initial = JsonParser.parseString("""
                {
                  "selected": {
                    "id": "rpg:skill_one"
                  },
                  "entries": [1, 2]
                }
                """).getAsJsonObject();
        UiLocalState state = new UiLocalState(initial);
        state.get("selected").getAsJsonObject().addProperty("id", "unexpected");
        assertEquals("rpg:skill_one", state.get("selected").getAsJsonObject().get("id").getAsString());

        var update = JsonParser.parseString("""
                {"id": "rpg:skill_two"}
                """);
        state.set("selected", update);
        update.getAsJsonObject().addProperty("id", "unexpected");
        assertEquals("rpg:skill_two", state.get("selected").getAsJsonObject().get("id").getAsString());
    }

    @Test
    public void preventsUnknownKeysAndTypeChanges() {
        UiLocalState state = new UiLocalState(JsonParser.parseString("""
                {
                  "tab": "skills",
                  "expanded": false,
                  "page": 1
                }
                """).getAsJsonObject());
        assertThrows(IllegalArgumentException.class, () -> state.set("unknown", new JsonPrimitive("test")));
        assertThrows(IllegalArgumentException.class, () -> state.set("tab", new JsonPrimitive(1)));
        assertThrows(IllegalArgumentException.class, () -> state.set("page", new JsonPrimitive("one")));
        assertThrows(IllegalArgumentException.class, () -> state.toggle("tab"));
        assertThrows(IllegalArgumentException.class, () -> state.validateSet("tab", new JsonPrimitive(false)));
        assertThrows(IllegalArgumentException.class, () -> state.validateToggle("page"));
        assertThrows(IllegalArgumentException.class, () -> new UiLocalState(JsonParser.parseString("""
                {"bad.key": true}
                """).getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> new UiLocalState(JsonParser.parseString("""
                {"unset": null}
                """).getAsJsonObject()));
    }

    @Test
    public void rebasingAfterResourceReloadPreservesOnlyCompatibleKeys() {
        UiLocalState current = new UiLocalState(JsonParser.parseString("""
                {
                  "tab": "skills",
                  "details": false,
                  "page": 1,
                  "filters": [1, 2],
                  "obsolete": "old"
                }
                """).getAsJsonObject());
        current.set("tab", new JsonPrimitive("inventory"));
        current.set("details", new JsonPrimitive(true));
        current.set("page", new JsonPrimitive(42));
        current.get("filters").getAsJsonArray().add(3);

        JsonObject updatedDefaults = JsonParser.parseString("""
                {
                  "tab": "default",
                  "details": true,
                  "page": "first",
                  "filters": ["all"],
                  "newOption": false
                }
                """).getAsJsonObject();
        UiLocalState updated = UiLocalState.rebased(updatedDefaults, current);

        assertEquals("inventory", updated.get("tab").getAsString());
        assertTrue(updated.get("details").getAsBoolean());
        assertEquals("first", updated.get("page").getAsString());
        assertEquals(2, updated.get("filters").getAsJsonArray().size());
        assertFalse(updated.get("newOption").getAsBoolean());
        assertThrows(IllegalArgumentException.class, () -> updated.get("obsolete"));
        updated.set("tab", new JsonPrimitive("reloaded"));
        assertEquals("inventory", current.get("tab").getAsString());
    }

    @Test
    public void rebasingRejectsInvalidNewStateWithoutModifyingPrevious() {
        UiLocalState original = new UiLocalState(JsonParser.parseString("""
                {
                  "tab": "changed"
                }
                """).getAsJsonObject());
        assertThrows(IllegalArgumentException.class, () -> UiLocalState.rebased(JsonParser.parseString("""
                {
                  "bad.key": true
                }
                """).getAsJsonObject(), original));
        assertEquals("changed", original.get("tab").getAsString());
    }

    @Test
    public void eachScreenGetsAnIndependentStateInstance() {
        var initial = JsonParser.parseString("""
                {
                  "tab": "skills"
                }
                """).getAsJsonObject();
        UiLocalState first = new UiLocalState(initial);
        UiLocalState second = new UiLocalState(initial);
        first.set("tab", new JsonPrimitive("inventory"));
        assertEquals("skills", second.get("tab").getAsString());
    }
}
