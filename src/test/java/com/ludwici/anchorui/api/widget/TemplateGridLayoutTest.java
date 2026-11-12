package com.ludwici.anchorui.api.widget;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.layout.TemplateListLayoutRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemplateGridLayoutTest {
    private record SpiralConfig(int step) {
        private static final Codec<SpiralConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("step").forGetter(SpiralConfig::step)
        ).apply(instance, SpiralConfig::new));
    }

    private static JsonObject object(String input) {
        return JsonParser.parseString(input).getAsJsonObject();
    }

    private static JsonArray items() {
        return JsonParser.parseString("""
                [
                  {"id":"rpg:one","title":"One"},
                  {"id":"rpg:two","title":"Two"},
                  {"id":"rpg:three","title":"Three"},
                  {"id":"rpg:four","title":"Four"},
                  {"id":"rpg:five","title":"Five"}
                ]
                """).getAsJsonArray();
    }

    private static JsonObject templateList() {
        return object("""
                {
                  "key": "id",
                  "rowHeight": 20,
                  "gap": 3,
                  "size": {
                    "width": 240,
                    "height": 100
                  },
                  "padding": {
                    "left": 8,
                    "right": 8
                  },
                  "template": {
                    "type": "anchorui:button",
                    "text": {
                      "item": "title"
                    },
                    "onClick": {
                      "type": "rpg:select",
                      "item": {
                        "item": "id"
                      }
                    }
                  }
                }
                """);
    }

    @Test
    public void arrangesGridRowsByColumnAndWrapsToNextLine() {
        JsonObject settings = templateList();
        settings.add("layout", object("""
                {
                  "type": "grid",
                  "columns": 3,
                  "columnGap": 7,
                  "rowGap": 5
                }
                """));
        List<TemplateListExpander.Entry> expanded = TemplateListExpander.expand(settings, items(), "rpg:skills", Map.of());

        int[][] expected = {{0, 0}, {77, 0}, {154, 0}, {0, 25}, {77, 25}};
        for (int index = 0; index < expanded.size(); index++) {
            JsonObject row = expanded.get(index).settings();
            JsonObject position = row.getAsJsonObject("position");
            JsonObject size = row.getAsJsonObject("size");
            assertEquals(expected[index][0], position.get("x").getAsInt());
            assertEquals(expected[index][1], position.get("y").getAsInt());
            assertEquals(70, size.get("width").getAsInt());
            assertEquals(20, size.get("height").getAsInt());
            assertEquals("rpg:skills/__content", position.get("relativeTo").getAsString());
            assertEquals("rpg:skills", row.get("__anchorui_container_parent").getAsString());
            assertEquals(items().get(index).getAsJsonObject().get("title").getAsString(), row.get("text").getAsString());
            assertEquals(items().get(index).getAsJsonObject().get("id").getAsString(),
                    row.getAsJsonObject("onClick").get("item").getAsString());
            assertEquals(items().get(index), expanded.get(index).itemContext().value());
        }
        assertNotEquals(expanded.get(0).settings().get("id"), expanded.get(1).settings().get("id"));
    }

    @Test
    public void honorsExplicitCellDimensionsAndTemplatePositionOffsets() {
        JsonObject settings = templateList();
        settings.remove("size");
        settings.add("layout", object("""
                {
                  "type": "anchorui:grid",
                  "columns": 2,
                  "columnGap": 4,
                  "rowGap": 6,
                  "cellWidth": 30,
                  "cellHeight": 18
                }
                """));
        settings.add("template", object("""
                {
                  "type": "anchorui:button",
                  "text": {
                    "item": "title"
                  },
                  "position": {
                    "x": 2,
                    "y": 3
                  },
                  "size": {
                    "width": 999,
                    "height": 99
                  }
                }
                """));

        List<TemplateListExpander.Entry> rows = TemplateListExpander.expand(settings, items(), "rpg:skills", Map.of());
        int[][] expected = {{2, 3}, {36, 3}, {2, 27}, {36, 27}, {2, 51}};
        for (int index = 0; index < rows.size(); index++) {
            JsonObject row = rows.get(index).settings();
            assertEquals(expected[index][0], row.getAsJsonObject("position").get("x").getAsInt());
            assertEquals(expected[index][1], row.getAsJsonObject("position").get("y").getAsInt());
            assertEquals(30, row.getAsJsonObject("size").get("width").getAsInt());
            assertEquals(18, row.getAsJsonObject("size").get("height").getAsInt());
        }
    }

    @Test
    public void keepsLegacyVerticalLayoutAndTemplateDimensionsWithoutLayoutSetting() {
        JsonObject settings = templateList();
        settings.add("template", object("""
                {
                  "type": "anchorui:button",
                  "text": "Example",
                  "position": {
                    "x": 4,
                    "y": 2
                  },
                  "size": {
                    "width": 130,
                    "height": 14
                  }
                }
                """));
        List<TemplateListExpander.Entry> rows = TemplateListExpander.expand(settings, items(), "rpg:quests", Map.of());
        for (int index = 0; index < rows.size(); index++) {
            JsonObject row = rows.get(index).settings();
            assertEquals(4, row.getAsJsonObject("position").get("x").getAsInt());
            assertEquals(2 + index * 23, row.getAsJsonObject("position").get("y").getAsInt());
            assertEquals(130, row.getAsJsonObject("size").get("width").getAsInt());
            assertEquals(14, row.getAsJsonObject("size").get("height").getAsInt());
        }
    }

    @Test
    public void acceptsJavaRegisteredCustomLayoutWithCodecOptions() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("template_layout_test", "diagonal");
        AtomicInteger calls = new AtomicInteger();
        TemplateListLayoutRegistry.register(id, SpiralConfig.CODEC, (options, metrics) -> index -> {
            calls.incrementAndGet();
            return new TemplateListLayoutRegistry.Cell(index * options.step(), index * 3, 40, 18);
        });
        JsonObject settings = templateList();
        settings.add("layout", object("""
                {
                  "type": "template_layout_test:diagonal",
                  "step": 12
                }
                """));

        List<TemplateListExpander.Entry> rows = TemplateListExpander.expand(settings, items(), "rpg:skills", Map.of());
        assertEquals(5, calls.get());
        assertEquals(48, rows.get(4).settings().getAsJsonObject("position").get("x").getAsInt());
        assertEquals(12, rows.get(4).settings().getAsJsonObject("position").get("y").getAsInt());
        assertEquals(40, rows.get(4).settings().getAsJsonObject("size").get("width").getAsInt());
        assertEquals(18, rows.get(4).settings().getAsJsonObject("size").get("height").getAsInt());
        assertThrows(IllegalArgumentException.class, () -> {
            JsonObject malformed = templateList();
            malformed.add("layout", object("""
                    {
                      "type": "template_layout_test:diagonal",
                      "step": "invalid"
                    }
                    """));
            TemplateListExpander.expand(malformed, items(), "rpg:skills", Map.of());
        });
        assertThrows(IllegalStateException.class, () -> TemplateListLayoutRegistry.register(id, (options, metrics) -> index -> new TemplateListLayoutRegistry.Cell(0, 0, 20, 20)));
    }

    @Test
    public void rejectsBadGridSettingsAndMissingWidth() {
        for (String options : List.of(
                "{\"type\":\"grid\",\"columns\":0}",
                "{\"type\":\"grid\",\"columns\":257}",
                "{\"type\":\"grid\",\"columns\":3,\"columnGap\":-1}",
                "{\"type\":\"grid\",\"columns\":3,\"cellHeight\":0}",
                "{\"type\":\"grid\",\"columns\":4,\"cellWidth\":-3}",
                "{\"type\":\"unknown:grid\"}"
        )) {
            JsonObject settings = templateList();
            settings.add("layout", JsonParser.parseString(options));
            assertThrows(IllegalArgumentException.class, () ->
                    TemplateListExpander.expand(settings, items(), "rpg:skills", Map.of()), options);
        }

        JsonObject noWidth = templateList();
        noWidth.remove("size");
        noWidth.add("layout", object("""
                {
                  "type": "grid",
                  "columns": 3
                }
                """));
        assertThrows(IllegalArgumentException.class, () ->
                TemplateListExpander.expand(noWidth, items(), "rpg:skills", Map.of()));
    }
}
