package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateListReuseTest {
    private static JsonObject json(String source) {
        return JsonParser.parseString(source).getAsJsonObject();
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
    public void changedIndexOrKeyRecreatesWidgetToKeepItemContextAccurate() {
        JsonObject definition = json("{\"type\":\"anchorui:button\"}");
        ItemContext previous = item("{\"id\":\"first\"}", "first", 0);
        ItemContext shifted = item("{\"id\":\"first\"}", "first", 1);
        ItemContext differentKey = item("{\"id\":\"first\"}", "other", 0);

        assertFalse(TemplateListReuse.canReuse(definition, definition, previous, shifted));
        assertFalse(TemplateListReuse.canReuse(definition, definition, previous, differentKey));
    }
}
