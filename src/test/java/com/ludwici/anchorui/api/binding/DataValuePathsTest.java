package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataValuePathsTest {
    @Test
    public void resolvesNestedFieldsAndListElements() {
        var doc = JsonParser.parseString("""
                {"stats":{"health":18,"alive":true},"quests":[{"title":"First","rewards":[{"name":"Gem"}]},{"title":"Second"}]}
                """);
        assertEquals(18, DataValuePaths.resolve(doc, DataValuePaths.tokens("stats.health")).getAsInt());
        assertTrue(DataValuePaths.resolve(doc, DataValuePaths.tokens("stats.alive")).getAsBoolean());
        assertEquals("Second", DataValuePaths.resolve(doc, DataValuePaths.tokens("quests[1].title")).getAsString());
        assertEquals("Gem", DataValuePaths.resolve(doc, DataValuePaths.tokens("quests[0].rewards[0].name")).getAsString());
        assertEquals(doc, DataValuePaths.resolve(doc, DataValuePaths.tokens("")));
    }

    @Test
    public void missingValuesReturnJsonNull() {
        var doc = JsonParser.parseString("{\"stats\":{\"health\":18},\"items\":[1]}");
        assertTrue(DataValuePaths.resolve(doc, DataValuePaths.tokens("stats.missing")).isJsonNull());
        assertTrue(DataValuePaths.resolve(doc, DataValuePaths.tokens("items[5]")).isJsonNull());
        assertTrue(DataValuePaths.resolve(doc, DataValuePaths.tokens("stats.health[0]")).isJsonNull());
    }

    @Test
    public void disallowsInvalidPaths() {
        for (String path : new String[]{".a", "a..b", "a.", "a[abc]", "a[-1]", "a[2", "a[0]oops", "a!b", "a[0]."}) {
            assertThrows(IllegalArgumentException.class, () -> DataValuePaths.tokens(path), path);
        }
        assertThrows(IllegalArgumentException.class, () -> DataValuePaths.tokens("x".repeat(257)));
    }
}
