package com.ludwici.anchorui.api.style;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.binding.BoundText;
import com.ludwici.anchorui.api.binding.DataBindingSpec;
import com.ludwici.anchorui.api.binding.DataValueRegistry;
import com.ludwici.anchorui.api.binding.NumberBindingSpec;
import com.ludwici.anchorui.api.widget.WidgetEditorRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OptionalJsonDefaultsTest {
    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    @Test
    public void buttonLabelCanBeOmitted() {
        var element = new LoreEntity.UiElement(json("""
                {"type":"anchorui:button","size":{"width":24,"height":24}}
                """));
        assertEquals("", element.text);
        assertFalse(element.settings.has("text"));
        var field = WidgetEditorRegistry.fields(WidgetTypeIds.BUTTON).stream().filter(entry -> entry.path().equals("text")).findFirst().orElseThrow();
        assertEquals("", field.fallback().getAsString());
    }

    @Test
    public void fallbackCanBeOmittedAndKeepsTheExpectedType() {
        JsonObject binding = json("""
                {"bind":"optional_defaults:missing"}
                """);
        assertEquals("", DataBindingSpec.parse(binding, new JsonPrimitive(""), DataBindingSpec.Kind.STRING).fallback().getAsString());
        assertEquals(100.0, NumberBindingSpec.parse(binding, 100).fallback());
        assertFalse(DataBindingSpec.parse(binding, new JsonPrimitive(false), DataBindingSpec.Kind.BOOLEAN).fallback().getAsBoolean());
        assertEquals(new JsonArray(), DataBindingSpec.parse(binding, new JsonArray(), DataBindingSpec.Kind.LIST).fallback());
        DataValueRegistry.register(ResourceLocation.fromNamespaceAndPath("optional_defaults", "missing"), context -> JsonNull.INSTANCE);
        assertEquals("", BoundText.parse(binding).get(null));
        binding.addProperty("fallback", "Unavailable");
        assertEquals("Unavailable", BoundText.parse(binding).get(null));
    }

    @Test
    public void eachCoordinateDefaultsIndependentlyToZero() {
        PositionElement neither = new PositionElement(new JsonObject());
        assertEquals(0, neither.x);
        assertEquals(0, neither.y);
        PositionElement onlyX = new PositionElement(json("""
                {"x":12}
                """));
        assertEquals(12, onlyX.x);
        assertEquals(0, onlyX.y);
        PositionElement onlyY = new PositionElement(json("""
                {"y":-8}
                """));
        assertEquals(0, onlyY.x);
        assertEquals(-8, onlyY.y);
        PositionElement multiple = new PositionElement(json("""
                {"anchors":[{"point":"LEFT","anchor":"LEFT","x":3},{"point":"RIGHT","anchor":"RIGHT","y":4}]}
                """));
        assertEquals(0, multiple.anchors.getFirst().y());
        assertEquals(0, multiple.anchors.getLast().x());
    }

    @Test
    public void textShadowDefaultsToFalseButAllowsExplicitOverrides() {
        assertFalse(new LoreStyle.Element(new JsonObject()).textShadow);
        assertTrue(new LoreStyle.Element(json("""
                {"shadow":true}
                """)).textShadow);
        LoreStyle style = new LoreStyle(json("""
                {"anchorui:text":{"text":{"shadow":true}}}
                """));
        assertTrue(style.get(WidgetTypeIds.TEXT, new JsonObject()).textShadow);
        assertFalse(style.get(WidgetTypeIds.TEXT, json("""
                {"shadow":false}
                """)).textShadow);
        var field = WidgetEditorRegistry.fields(WidgetTypeIds.TEXT).stream().filter(entry -> entry.path().equals("shadow")).findFirst().orElseThrow();
        assertFalse(field.fallback().getAsBoolean());
    }
}
