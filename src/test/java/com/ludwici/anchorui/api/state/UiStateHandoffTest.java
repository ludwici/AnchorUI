package com.ludwici.anchorui.api.state;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UiStateHandoffTest {
    @Test
    public void explicitlySelectedKeysTransferWithOptionalRenaming() {
        UiLocalState source = new UiLocalState(JsonParser.parseString("""
                {
                  "tab": "skills",
                  "filter": "all",
                  "hidden": true,
                  "nested": {"id": "first"}
                }
                """).getAsJsonObject());
        UiLocalState destination = new UiLocalState(JsonParser.parseString("""
                {
                  "selectedTab": "default",
                  "filter": "none",
                  "hidden": false,
                  "nested": {"id": "default"}
                }
                """).getAsJsonObject());
        source.set("tab", JsonParser.parseString("\"inventory\""));
        var settings = JsonParser.parseString("""
                {
                  "stateHandoff": {
                    "selectedTab": "tab",
                    "nested": "nested"
                  }
                }
                """).getAsJsonObject();

        Map<String, String> handoff = UiStateHandoff.parse(settings, source);
        UiStateHandoff.copy(source, destination, handoff);
        assertEquals("inventory", destination.get("selectedTab").getAsString());
        assertEquals("first", destination.get("nested").getAsJsonObject().get("id").getAsString());
        assertEquals("none", destination.get("filter").getAsString());
        assertFalse(destination.get("hidden").getAsBoolean());

        source.get("nested").getAsJsonObject().addProperty("id", "tampered");
        assertEquals("first", destination.get("nested").getAsJsonObject().get("id").getAsString());
    }

    @Test
    public void noHandoffLeavesNewScreenAtDefaults() {
        UiLocalState source = new UiLocalState(JsonParser.parseString("""
                {"tab": "changed"}
                """).getAsJsonObject());
        UiLocalState target = new UiLocalState(JsonParser.parseString("""
                {"tab": "default"}
                """).getAsJsonObject());
        UiStateHandoff.copy(source, target, UiStateHandoff.parse(new com.google.gson.JsonObject(), source));
        assertEquals("default", target.get("tab").getAsString());
    }

    @Test
    public void invalidTargetDoesNotPartiallyChangeDestination() {
        UiLocalState source = new UiLocalState(JsonParser.parseString("""
                {"page": 2, "tab": "inventory"}
                """).getAsJsonObject());
        UiLocalState target = new UiLocalState(JsonParser.parseString("""
                {"page": 1, "tab": false}
                """).getAsJsonObject());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> UiStateHandoff.copy(source, target, Map.of("page", "page", "tab", "tab")));
        assertEquals(1, target.get("page").getAsInt());
        assertFalse(target.get("tab").getAsBoolean());
        assertEquals(true, exception.getMessage().contains("Cannot transfer UI state"));
    }

    @Test
    public void malformedHandoffIsRejectedDuringActionCompilation() {
        UiLocalState source = new UiLocalState(JsonParser.parseString("""
                {"tab": "skills"}
                """).getAsJsonObject());
        assertThrows(IllegalArgumentException.class, () -> UiStateHandoff.parse(JsonParser.parseString("""
                {"stateHandoff": ["tab"]}
                """).getAsJsonObject(), source));
        assertThrows(IllegalArgumentException.class, () -> UiStateHandoff.parse(JsonParser.parseString("""
                {"stateHandoff": {"page": "unknown"}}
                """).getAsJsonObject(), source));
        assertThrows(IllegalArgumentException.class, () -> UiStateHandoff.parse(JsonParser.parseString("""
                {"stateHandoff": {"page": 12}}
                """).getAsJsonObject(), source));
        assertThrows(IllegalArgumentException.class, () -> UiStateHandoff.parse(JsonParser.parseString("""
                {"stateHandoff": {"page": ""}}
                """).getAsJsonObject(), source));
    }

    @Test
    public void typeMismatchRejectsDestinationForNumbersAndCollections() {
        UiLocalState source = new UiLocalState(JsonParser.parseString("""
                {"page": 4, "options": [1, 2]}
                """).getAsJsonObject());
        UiLocalState destination = new UiLocalState(JsonParser.parseString("""
                {"page": "1", "options": {}}
                """).getAsJsonObject());
        assertThrows(IllegalArgumentException.class,
                () -> UiStateHandoff.copy(source, destination, Map.of("page", "page")));
        assertThrows(IllegalArgumentException.class,
                () -> UiStateHandoff.copy(source, destination, Map.of("options", "options")));
    }
}
