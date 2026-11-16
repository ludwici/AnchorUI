package com.ludwici.anchorui.api.style;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.widget.WidgetDocumentResolver;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiAnchorStyleMergeTest {
    private static JsonObject json(String source) {
        return JsonParser.parseString(source).getAsJsonObject();
    }

    @Test
    public void explicitMultipleAnchorsReplaceLegacyStylePositionFields() {
        LoreStyle style = new LoreStyle(json("""
                {
                  "anchorui:panel": {
                    "position": {
                      "point": "CENTER",
                      "anchor": "CENTER",
                      "x": 100
                    },
                    "size": {
                      "width": 210,
                      "height": 30
                    }
                  }
                }
                """));
        LoreStyle.Element overridden = style.get(ResourceLocation.parse("anchorui:panel"), json("""
                {
                  "position": {
                    "anchors": [
                      {"point":"LEFT","anchor":"LEFT","x":10},
                      {"point":"RIGHT","anchor":"RIGHT","x":-10}
                    ]
                  },
                  "size": {
                    "height": 40
                  }
                }
                """));
        assertFalse(overridden.resolvedSettings.getAsJsonObject("position").has("point"));
        assertFalse(overridden.resolvedSettings.getAsJsonObject("position").has("x"));
        assertEquals(2, overridden.position.anchors.size());
        assertTrue(overridden.position.dynamicWidth());
        assertEquals(210, overridden.size.width);
        assertEquals(40, overridden.size.height);
    }

    @Test
    public void legacyPositionOverrideCanReplaceInheritedMultiAnchorStyle() {
        LoreStyle style = new LoreStyle(json("""
                {
                  "anchorui:panel": {
                    "position": {
                      "anchors": [
                        {"point":"TOPLEFT","anchor":"TOPLEFT"},
                        {"point":"TOPRIGHT","anchor":"TOPRIGHT"}
                      ]
                    }
                  }
                }
                """));
        LoreStyle.Element result = style.get(ResourceLocation.parse("anchorui:panel"), json("""
                {
                  "position": {
                    "point":"CENTER",
                    "anchor":"CENTER"
                  }
                }
                """));
        assertFalse(result.resolvedSettings.getAsJsonObject("position").has("anchors"));
        assertEquals(1, result.position.anchors.size());
        assertEquals(ElementAnchor.CENTER, result.position.point);
    }

    @Test
    public void templateReferenceCanReplaceSingleAnchorPositionWithMultipleConstraints() {
        Map<String, JsonObject> definitions = Map.of("demo:base", json("""
                {
                  "type":"anchorui:panel",
                  "position":{"point":"TOPLEFT","anchor":"TOPLEFT","x":30}
                }
                """));
        JsonObject resolved = WidgetDocumentResolver.resolve(json("""
                {
                  "entities":[{
                    "ref":"demo:base",
                    "id":"demo:overridden",
                    "position":{
                      "anchors":[
                        {"point":"LEFT","anchor":"LEFT","relativeTo":"screen","x":20},
                        {"point":"RIGHT","anchor":"RIGHT","relativeTo":"screen","x":-20}
                      ]
                    }
                  }]
                }
                """), "demo", definitions);
        JsonObject position = resolved.getAsJsonArray("entities").get(0).getAsJsonObject().getAsJsonObject("position");
        assertFalse(position.has("point"));
        assertFalse(position.has("x"));
        assertEquals(2, new PositionElement(position).anchors.size());
    }
}
