package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateListReuseTest {
    private static JsonObject json(String source) {
        return JsonParser.parseString(source).getAsJsonObject();
    }

    private static JsonArray array(String source) {
        return JsonParser.parseString(source).getAsJsonArray();
    }

    private static ItemContext item(String value, String key, int index) {
        return new ItemContext(json(value), key, index);
    }

    @Test
    public void unchangedKeyAndDataRetainWidget() {
        JsonObject definition = json("{\"type\":\"anchorui:button\",\"text\":\"Quest\"}");
        ItemContext previous = item("{\"id\":\"first\",\"title\":\"Quest\"}", "first", 0);
        ItemContext latest = item("{\"id\":\"first\",\"title\":\"Quest\"}", "first", 0);

        assertTrue(TemplateListReuse.canReuse(definition, definition.deepCopy(), previous, latest));
        assertTrue(TemplateListReuse.canReuse(definition, definition.deepCopy(), null, null));
    }

    @Test
    public void changedContentOrTemplateRecreatesWidget() {
        JsonObject definition = json("{\"type\":\"anchorui:button\",\"text\":\"Quest\"}");
        JsonObject modifiedDefinition = json("{\"type\":\"anchorui:button\",\"text\":\"Updated\"}");
        ItemContext previous = item("{\"id\":\"first\",\"title\":\"Quest\"}", "first", 0);
        ItemContext changed = item("{\"id\":\"first\",\"title\":\"Updated\"}", "first", 0);

        assertFalse(TemplateListReuse.canReuse(definition, modifiedDefinition, previous, previous));
        assertFalse(TemplateListReuse.canReuse(definition, definition, previous, changed));
        assertFalse(TemplateListReuse.canReuse(definition, definition, null, previous));
    }

    @Test
    public void changedIndexPreservesTheSameRowAndLiveContext() {
        JsonObject definition = json("{\"type\":\"anchorui:button\"}");
        ItemContext previous = item("{\"id\":\"first\"}", "first", 0);
        ItemContext shifted = item("{\"id\":\"first\"}", "first", 1);

        assertTrue(TemplateListReuse.canReuse(definition, definition, previous, shifted));
        WidgetContext context = new WidgetContext(null, null, null, null).withItemContext(previous);
        previous.moveToIndex(shifted.index());
        assertSame(previous, context.itemContext());
        assertEquals(1, context.itemContext().index());
        assertEquals("first", context.itemContext().get("id").getAsString());
        assertEquals(shifted, context.itemContext());
        assertThrows(IllegalArgumentException.class, () -> previous.moveToIndex(-1));
    }

    @Test
    public void changingKeyOrSourceDataDisallowsReuse() {
        JsonObject definition = json("{\"type\":\"anchorui:button\"}");
        ItemContext previous = item("{\"id\":\"first\"}", "first", 0);
        ItemContext differentKey = item("{\"id\":\"first\"}", "other", 1);
        ItemContext differentValue = item("{\"id\":\"other\"}", "first", 1);
        assertFalse(TemplateListReuse.canReuse(definition, definition, previous, differentKey));
        assertFalse(TemplateListReuse.canReuse(definition, definition, previous, differentValue));
    }

    @Test
    public void generatedLayoutCoordinatesMayChangeButOtherSettingsMayNot() {
        JsonObject before = json("""
                {"type":"anchorui:button","text":"Quest",
                 "position":{"x":0,"y":0,"relativeTo":"example:quests/__content"}}
                """);
        JsonObject after = json("""
                {"type":"anchorui:button","text":"Quest",
                 "position":{"x":16,"y":24,"relativeTo":"example:quests/__content"}}
                """);
        ItemContext previous = item("{\"id\":\"first\"}", "first", 0);
        ItemContext shifted = item("{\"id\":\"first\"}", "first", 1);
        assertTrue(TemplateListReuse.canReuse(before, after, previous, shifted));
        after.getAsJsonObject("position").addProperty("relativeTo", "example:elsewhere");
        assertFalse(TemplateListReuse.canReuse(before, after, previous, shifted));
        after.getAsJsonObject("position").addProperty("relativeTo", "example:quests/__content");
        after.addProperty("text", "Changed");
        assertFalse(TemplateListReuse.canReuse(before, after, previous, shifted));
    }

    @Test
    public void reorderedExpandedRowsKeepTheirStableIdsAndCanReuseWidgets() {
        JsonObject list = json("""
                {"key":"id","rowHeight":22,"gap":2,
                 "template":{"type":"anchorui:button","text":{"item":"title"}}}
                """);
        List<TemplateListExpander.Entry> before = TemplateListExpander.expand(list, array("""
                [{"id":"first","title":"First"},{"id":"second","title":"Second"}]
                """), "example:quests", Map.of());
        List<TemplateListExpander.Entry> after = TemplateListExpander.expand(list, array("""
                [{"id":"second","title":"Second"},{"id":"first","title":"First"}]
                """), "example:quests", Map.of());

        TemplateListExpander.Entry originalFirst = before.get(0);
        TemplateListExpander.Entry movedFirst = after.get(1);
        assertEquals(originalFirst.settings().get("id"), movedFirst.settings().get("id"));
        assertEquals(0, originalFirst.settings().getAsJsonObject("position").get("y").getAsInt());
        assertEquals(24, movedFirst.settings().getAsJsonObject("position").get("y").getAsInt());
        assertTrue(TemplateListReuse.canReuse(originalFirst.settings(), movedFirst.settings(), originalFirst.itemContext(), movedFirst.itemContext()));
    }
}
