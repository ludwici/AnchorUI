package com.ludwici.anchorui.client.gui.editor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.WidgetTypeIds;
import com.ludwici.anchorui.api.widget.WidgetEditorRegistry;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppearanceInspectorControlsTest {
    private static JsonObject json(String input) {
        return JsonParser.parseString(input).getAsJsonObject();
    }

    private static AppearanceInspectorControls.Control find(List<AppearanceInspectorControls.Control> controls, String path) {
        return controls.stream().filter(value -> value.field().path().equals(path)).findFirst().orElseThrow();
    }

    @Test
    public void builtinFieldsExposeTextColorsAndTextureVariants() {
        var offsets = new HudLayoutOffsets();
        var id = "example:checkbox";
        var source = json("""
                {"type":"anchorui:checkbox","text":"Enabled",
                 "fillColor":"#AABBCC","textures":{"background":{"normal":"example:ui/button"}}}
                """);
        var controls = AppearanceInspectorControls.describe(id, WidgetTypeIds.CHECKBOX, source, offsets);
        assertEquals("Enabled", find(controls, "text").value());
        assertEquals("#AABBCC", find(controls, "fillColor").value());
        assertEquals("example:ui/button", find(controls, "textures.background.normal").value());
        assertEquals("", find(controls, "textures.background.hover").value());
        assertTrue(find(controls, "text").textual());
        offsets.setSettingOverride(id, "appearance", "text", new JsonPrimitive("Changed"));
        assertTrue(find(AppearanceInspectorControls.describe(id, WidgetTypeIds.CHECKBOX, source, offsets), "text").modified());
        assertEquals("Changed", find(AppearanceInspectorControls.describe(id, WidgetTypeIds.CHECKBOX, source, offsets), "text").value());
    }

    @Test
    public void textBindingsAreDisplayedAndPreservedByEditor() {
        String id = "example:clock";
        var source = json("{\"type\":\"anchorui:text\",\"text\":{\"bind\":\"example:clock\",\"fallback\":\"\"}}");
        var offsets = new HudLayoutOffsets();
        var control = find(AppearanceInspectorControls.describe(id, WidgetTypeIds.TEXT, source, offsets), "text");
        assertEquals(source.get("text").toString(), control.value());
        assertEquals(source.get("text"), AppearanceInspectorControls.parseEditorValue(control.field(), control.value()));
        var changed = json("{\"bind\":\"example:new_clock\",\"fallback\":\"?\"}");
        offsets.setSettingOverride(id, "appearance", "text", changed);
        var saved = HudLayoutOffsets.fromJson(offsets.toJson());
        assertEquals(changed, saved.widgetSettings(id, source).get("text"));
        assertEquals(changed.toString(), find(AppearanceInspectorControls.describe(id, WidgetTypeIds.TEXT, source, saved), "text").value());
        assertEquals(new JsonPrimitive("Plain"), AppearanceInspectorControls.parseEditorValue(control.field(), "Plain"));
    }

    @Test
    public void validatesAppearancePropertiesAndStepsTypedValues() {
        var color = WidgetEditorRegistry.Field.color("fillColor", "Fill", "#112233");
        assertEquals(WidgetEditorRegistry.Kind.COLOR, color.kind());
        var texture = WidgetEditorRegistry.Field.texture("texture", "Texture");
        var text = WidgetEditorRegistry.Field.text("text", "Label", "");
        assertEquals("#FFAA22", AppearanceInspectorControls.parse(color, "#ffaa22").getAsString());
        assertThrows(IllegalArgumentException.class, () -> AppearanceInspectorControls.parse(color, "red"));
        assertEquals("example:ui/skin", AppearanceInspectorControls.parse(texture, "example:ui/skin").getAsString());
        assertThrows(IllegalArgumentException.class, () -> AppearanceInspectorControls.parse(texture, "Invalid Texture"));
        assertEquals("Hello world", AppearanceInspectorControls.parse(text, "Hello world").getAsString());
        assertThrows(IllegalArgumentException.class, () -> AppearanceInspectorControls.parse(text, "x".repeat(4097)));

        var width = WidgetEditorRegistry.Field.integer("size.width", "Width", 120, 0, 200, 10);
        assertEquals(200, AppearanceInspectorControls.stepped(new AppearanceInspectorControls.Control(width, "200", false), 1).getAsInt());
        assertEquals(190, AppearanceInspectorControls.stepped(new AppearanceInspectorControls.Control(width, "200", false), -1).getAsInt());
        assertThrows(IllegalArgumentException.class, () -> AppearanceInspectorControls.parse(width, "-5"));

        var toggle = WidgetEditorRegistry.Field.bool("showText", "Show values", false);
        assertTrue(new AppearanceInspectorControls.Control(toggle, "false", false).booleanField());
        assertFalse(new AppearanceInspectorControls.Control(width, "100", false).booleanField());
        assertTrue(AppearanceInspectorControls.stepped(new AppearanceInspectorControls.Control(toggle, "false", false), 1).getAsBoolean());
        assertFalse(AppearanceInspectorControls.parse(toggle, "FALSE").getAsBoolean());
        assertThrows(IllegalArgumentException.class, () -> AppearanceInspectorControls.parse(toggle, "yes"));
        var align = WidgetEditorRegistry.Field.choice("align", "Alignment", "left", "left", "center", "right");
        assertEquals("center", AppearanceInspectorControls.stepped(new AppearanceInspectorControls.Control(align, "left", false), 1).getAsString());
        assertThrows(IllegalArgumentException.class, () -> AppearanceInspectorControls.parse(align, "middle"));
    }

    @Test
    public void actionButtonsExposeSkinsAndHoverPaletteWithoutAffectingVanillaDefaults() {
        var controls = AppearanceInspectorControls.describe("test:button", WidgetTypeIds.BUTTON, json("""
                        {"type":"anchorui:button","text":"Click me","size":{"width":120,"height":20}}
                        """), new HudLayoutOffsets());
        assertEquals("Click me", find(controls, "text").value());
        assertEquals("#FF28384B", find(controls, "fillColor").value());
        assertEquals("#FF385E81", find(controls, "hoverColor").value());
        assertEquals("#FF353B43", find(controls, "disabledColor").value());
        assertEquals("", find(controls, "textures.background.hover").value());
        assertTrue(find(controls, "textures.background.normal").textual());
        assertEquals(120, Integer.parseInt(find(controls, "size.width").value()));
    }

    @Test
    public void appearanceOverridesMergeAsSafeWidgetFieldsAndSurviveStorage() {
        var id = "example:panel";
        HudLayoutOffsets offsets = new HudLayoutOffsets();
        offsets.setSettingOverride(id, "appearance", "fillColor", new JsonPrimitive("#FF112233"));
        offsets.setSettingOverride(id, "appearance", "texture", new JsonPrimitive("example:ui/window"));
        offsets.setSettingOverride(id, "appearance", "size.width", new JsonPrimitive(260));
        offsets.setSettingOverride(id, "layout", "gap", new JsonPrimitive(8));
        var source = json("""
                {"id":"example:panel","type":"anchorui:panel",
                 "size":{"width":180,"height":80},"fillColor":"#FF001122","onClick":{"type":"some:action"}}
                """);

        HudLayoutOffsets saved = HudLayoutOffsets.fromJson(offsets.toJson());
        assertTrue(saved.sameAs(offsets));
        var effective = saved.widgetSettings(id, source);
        assertEquals("#FF112233", effective.get("fillColor").getAsString());
        assertEquals("example:ui/window", effective.get("texture").getAsString());
        assertEquals(260, effective.getAsJsonObject("size").get("width").getAsInt());
        assertEquals(80, effective.getAsJsonObject("size").get("height").getAsInt());
        assertEquals(8, saved.effectiveSettings(id, source).getAsJsonObject("layout").get("gap").getAsInt());
        assertEquals("#FF001122", source.get("fillColor").getAsString());

        saved.clearSettingOverride(id, "appearance", "fillColor");
        assertEquals("#FF001122", saved.widgetSettings(id, source).get("fillColor").getAsString());
        saved.reset(id);
        assertEquals(180, saved.widgetSettings(id, source).getAsJsonObject("size").get("width").getAsInt());
    }

    @Test
    public void registrationWhitelistsEditableFieldsAndBlocksEventOverrideInjection() {
        var type = ResourceLocation.fromNamespaceAndPath("editor_tests", "custom");
        WidgetEditorRegistry.register(type, List.of(WidgetEditorRegistry.Field.text("caption", "Caption", "Hello"), WidgetEditorRegistry.Field.integer("width", "Width", 40, 0, 300, 1)));
        assertEquals("caption", WidgetEditorRegistry.fields(type).getFirst().path());
        assertThrows(IllegalStateException.class, () -> WidgetEditorRegistry.register(type, List.of()));
        assertThrows(IllegalArgumentException.class, () -> WidgetEditorRegistry.Field.text("onClick", "Action", ""));
        assertThrows(IllegalArgumentException.class, () -> WidgetEditorRegistry.Field.text("position.x", "Position", ""));

        HudLayoutOffsets offsets = new HudLayoutOffsets();
        var id = "editor_tests:custom_instance";
        offsets.setSettingOverride(id, "appearance", "caption", new JsonPrimitive("World"));
        offsets.setSettingOverride(id, "appearance", "onClick", new JsonPrimitive("hacked"));
        var source = json("""
                {"type":"editor_tests:custom","caption":"Hello",
                "onClick":{"type":"editor_tests:action"}}
                """);
        var effective = offsets.widgetSettings(id, source);
        assertEquals("World", effective.get("caption").getAsString());
        assertEquals("editor_tests:action", effective.getAsJsonObject("onClick").get("type").getAsString());
    }
}
