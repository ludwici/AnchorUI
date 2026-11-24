package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.state.UiLocalState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EditorStateValuesTest {
    @Test
    public void offersTypedLiteralStateActionsWithoutExecutingThem() {
        JsonObject definition = JsonParser.parseString("""
                {"children":[{"onClick":{"type":"anchorui:set_state","key":"page","value":"profile"}},{"onClick":{"type":"anchorui:set_state","key":"page","value":4}},{"onClick":{"type":"anchorui:set_state","key":"other","value":"ignore"}}]}
                """).getAsJsonObject();
        assertEquals(List.of("\"home\"", "\"profile\""), EditorStateValues.choices("page", new JsonPrimitive("home"), new JsonPrimitive("home"), List.of(definition)));
        assertEquals(List.of("false", "true"), EditorStateValues.choices("details", new JsonPrimitive(false), new JsonPrimitive(true), List.of()));
    }

    @Test
    public void previewStateIsIndependentOfRuntimeAndRetainsTypes() {
        JsonObject initial = JsonParser.parseString("""
                {"page":"home","count":1}
                """).getAsJsonObject();
        UiLocalState runtime = new UiLocalState(initial);
        UiLocalState preview = new UiLocalState(runtime.snapshot());
        preview.set("page", EditorStateValues.parseInput(preview.get("page"), "profile"));
        preview.set("count", EditorStateValues.parseInput(preview.get("count"), "2"));
        assertEquals("home", runtime.get("page").getAsString());
        assertEquals("profile", preview.get("page").getAsString());
        assertEquals(2, preview.get("count").getAsInt());
        assertThrows(IllegalArgumentException.class, () -> preview.set("count", EditorStateValues.parseInput(preview.get("count"), "false")));
    }
}
