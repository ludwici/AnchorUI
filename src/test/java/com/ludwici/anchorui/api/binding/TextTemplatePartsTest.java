package com.ludwici.anchorui.api.binding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextTemplatePartsTest {
    @Test
    public void splitsLiteralsAndBoundSources() {
        var parsed = TextTemplateParts.parse("<b>Hello {{anchorui:player.name}}</b> in {{anchorui:player.profile#name}}!");
        assertEquals(5, parsed.size());
        assertFalse(parsed.get(0).dynamic());
        assertEquals("<b>Hello ", parsed.get(0).text());
        assertTrue(parsed.get(1).dynamic());
        assertEquals("anchorui:player.name", parsed.get(1).source());
        assertEquals("", parsed.get(1).path());
        assertEquals("name", parsed.get(3).path());
        assertEquals("!", parsed.get(4).text());
    }

    @Test
    public void parsesFormattersInTemplates() {
        var parts = TextTemplateParts.parse("Mana {{rpg:stats#mana.current|rpg:mana}} XP {{rpg:exp|anchorui:percent}}");
        assertEquals(4, parts.size());
        assertEquals("rpg:stats", parts.get(1).source());
        assertEquals("mana.current", parts.get(1).path());
        assertEquals("rpg:mana", parts.get(1).format());
        assertEquals("anchorui:percent", parts.get(3).format());
    }

    @Test
    public void preservesStaticTextWhenNoBindingIsPresent() {
        var parts = TextTemplateParts.parse("<i>Static only</i>");
        assertEquals(1, parts.size());
        assertEquals("<i>Static only</i>", parts.getFirst().text());
        assertFalse(parts.getFirst().dynamic());
    }

    @Test
    public void validatesPathInsideTemplate() {
        assertThrows(IllegalArgumentException.class, () -> TextTemplateParts.parse("{{example:profile#bad..field}}"));
    }
}
