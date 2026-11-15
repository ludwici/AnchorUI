package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.PositionElement;
import com.ludwici.anchorui.client.gui.editor.LayoutInspectorControls;
import com.ludwici.anchorui.client.hud.HudLayoutOffsets;
import com.ludwici.anchorui.client.hud.HudLayoutStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StageOneTwoLayoutIntegrationTest {
    @TempDir
    Path directory;

    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    private static AnchorLayout.Frame frame(String id, String parent, int x, int y, int width, int height) {
        return new AnchorLayout.Frame(id,
                new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, parent, x, y), width, height);
    }

    private static ContainerLayout.Result resolve(int screenWidth, List<AnchorLayout.Frame> frames,
                                                  String parent, JsonObject layout, Set<String> hidden,
                                                  Map<String, StackLayout.Rules> rules) {
        return ContainerLayout.resolve(screenWidth, 300, frames,
                Map.of(parent, new PaddingElement()),
                Map.of(),
                Map.of("demo:first", parent, "demo:middle", parent, "demo:last", parent),
                Map.of(parent, ContainerAutoLayout.parse(layout)),
                rules, hidden, Set.of(),
                warning -> {
                    throw new AssertionError(warning);
                });
    }

    @Test
    public void rowHiddenChildrenReserveSlotsByDefaultAndCollapseOnlyWhenOptedIn() {
        String parent = "demo:panel";
        var frames = List.of(frame(parent, "screen", 0, 0, 200, 60),
                frame("demo:first", parent, 0, 0, 40, 15),
                frame("demo:middle", parent, 0, 0, 50, 15),
                frame("demo:last", parent, 0, 0, 60, 15));
        var reserve = resolve(300, frames, parent, json("""
                {
                  "layout": {
                    "type": "row",
                    "gap": 10
                  }
                }
                """), Set.of("demo:middle"), Map.of());
        var collapse = resolve(300, frames, parent, json("""
                {
                  "layout": {
                    "type": "row",
                    "gap": 10,
                    "collapseHidden": true
                  }
                }
                """), Set.of("demo:middle"), Map.of());
        assertEquals(110, reserve.bounds().get("demo:last").x());
        assertEquals(50, collapse.bounds().get("demo:last").x());
        assertEquals(110, resolve(300, frames, parent, json("""
                {
                  "layout": {
                    "type": "row",
                    "gap": 10,
                    "collapseHidden": true
                  }
                }
                """), Set.of(), Map.of()).bounds().get("demo:last").x());
    }

    @Test
    public void gridHiddenCellsReserveIndexOrReflowWithoutBreakingSurvivingChildIds() {
        String parent = "demo:panel";
        var frames = List.of(frame(parent, "screen", 15, 10, 200, 100),
                frame("demo:first", parent, 0, 0, 40, 20),
                frame("demo:middle", parent, 0, 0, 50, 20),
                frame("demo:last", parent, 0, 0, 60, 20));
        var reserved = resolve(300, frames, parent, json("""
                {
                  "layout": {
                    "type": "grid",
                    "columns": 2,
                    "gap": 4
                  }
                }
                """), Set.of("demo:middle"), Map.of());
        var collapsed = resolve(300, frames, parent, json("""
                {
                  "layout": {
                    "type": "grid",
                    "columns": 2,
                    "gap": 4,
                    "collapseHidden": true
                  }
                }
                """), Set.of("demo:middle"), Map.of());
        assertTrue(reserved.bounds().get("demo:last").y() > reserved.bounds().get("demo:first").y());
        assertEquals(collapsed.bounds().get("demo:first").y(), collapsed.bounds().get("demo:last").y());
        assertEquals(3, collapsed.bounds().keySet().stream()
                .filter(id -> id.startsWith("demo:") && !id.equals(parent)).count());
    }

    @Test
    public void hiddenManualChildDoesNotExpandAutoSizedParentWhenCollapseIsEnabled() {
        String parent = "demo:panel";
        var frames = List.of(frame(parent, "screen", 10, 10, 1, 1),
                frame("demo:first", parent, 0, 0, 40, 12),
                frame("demo:middle", parent, 100, 40, 80, 30),
                frame("demo:last", parent, 0, 0, 20, 12));
        var parents = Map.of("demo:first", parent, "demo:middle", parent, "demo:last", parent);
        var rules = Map.of("demo:middle", StackLayout.Rules.parse(json("""
                {
                  "layoutItem": false
                }
                """)));
        var automatic = ContainerAutoLayout.parse(json("""
                {
                  "layout": {
                    "type": "row",
                    "collapseHidden": true
                  }
                }
                """));
        var without = ContainerLayout.resolve(300, 250, frames,
                Map.of(parent, new PaddingElement()),
                Map.of(parent, new ContainerLayout.AutoSize(true, true)),
                parents, Map.of(parent, automatic), rules, Set.of("demo:middle"), Set.of(),
                warning -> {
                    throw new AssertionError(warning);
                });
        var with = ContainerLayout.resolve(300, 250, frames,
                Map.of(parent, new PaddingElement()),
                Map.of(parent, new ContainerLayout.AutoSize(true, true)),
                parents, Map.of(parent, automatic), rules, Set.of(), Set.of(),
                warning -> {
                    throw new AssertionError(warning);
                });
        assertEquals(60, without.sizes().get(parent).width());
        assertEquals(180, with.sizes().get(parent).width());
        assertTrue(with.sizes().get(parent).height() > without.sizes().get(parent).height());
    }

    @Test
    public void bundledPolicyDemoShowsReserveAndCollapseUsingOneConditionalState() throws Exception {
        var input = getClass().getClassLoader().getResourceAsStream(
                "assets/anchorui/anchorui/screens/hidden_space_policy_demo.json");
        org.junit.jupiter.api.Assertions.assertNotNull(input);
        try (var reader = new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)) {
            JsonObject doc = JsonParser.parseReader(reader).getAsJsonObject();
            assertFalse(doc.getAsJsonObject("state").get("hideMiddle").getAsBoolean());
            var entities = doc.getAsJsonArray("entities");
            JsonObject reserved = entities.get(4).getAsJsonObject();
            JsonObject collapsed = entities.get(5).getAsJsonObject();
            assertFalse(reserved.getAsJsonObject("layout").get("collapseHidden").getAsBoolean());
            assertTrue(collapsed.getAsJsonObject("layout").get("collapseHidden").getAsBoolean());
            for (JsonObject panel : List.of(reserved, collapsed)) {
                assertEquals("vertical", panel.getAsJsonObject("scroll").get("axis").getAsString());
                var middle = panel.getAsJsonArray("children").get(1).getAsJsonObject();
                assertEquals("hideMiddle", middle.getAsJsonObject("visibleWhen")
                        .getAsJsonObject("not").get("path").getAsString());
            }
        }
    }

    @Test
    public void editorOverridesSaveCancelAndResizeWithoutLeakingAcrossDrafts() throws Exception {
        String parent = "demo:panel";
        JsonObject base = json("""
                {
                  "type": "anchorui:panel",
                  "layout": {
                    "type": "row",
                    "gap": 4,
                    "collapseHidden": false
                  }
                }
                """);
        HudLayoutOffsets saved = new HudLayoutOffsets();
        HudLayoutOffsets draft = saved.copy();
        var controls = LayoutInspectorControls.describe(parent, base, draft);
        var wrapControl = controls.stream().filter(c -> c.key().equals("layout.wrap")).findFirst().orElseThrow();
        var collapseControl = controls.stream().filter(c -> c.key().equals("layout.collapseHidden")).findFirst().orElseThrow();
        draft.setSettingOverride(parent, "layout", "wrap", LayoutInspectorControls.stepped(wrapControl, 1));
        draft.setSettingOverride(parent, "layout", "collapseHidden",
                LayoutInspectorControls.stepped(collapseControl, 1));
        draft.setSettingOverride(parent, "layout", "gap", new JsonPrimitive(8));
        assertFalse(saved.hasSettingOverrides(parent));

        Path file = directory.resolve("screen-layout.json");
        HudLayoutStorage.save(file, draft);
        HudLayoutOffsets loaded = HudLayoutStorage.load(file);
        assertEquals(draft.toJson(), loaded.toJson());
        assertTrue(loaded.effectiveSettings(parent, base).getAsJsonObject("layout").get("wrap").getAsBoolean());
        assertTrue(loaded.effectiveSettings(parent, base).getAsJsonObject("layout").get("collapseHidden").getAsBoolean());

        var layout = ContainerAutoLayout.parse(loaded.effectiveSettings(parent, base));
        var frames = List.of(frame(parent, "screen", 0, 0, 110, 120),
                frame("demo:first", parent, 0, 0, 65, 20),
                frame("demo:middle", parent, 0, 0, 55, 20),
                frame("demo:last", parent, 0, 0, 40, 20));
        Map<String, String> parents = Map.of("demo:first", parent,
                "demo:middle", parent, "demo:last", parent);
        var narrow = ContainerLayout.resolve(240, 200, frames,
                Map.of(parent, new PaddingElement()), Map.of(), parents,
                Map.of(parent, layout), Set.of("demo:middle"), Set.of(), warning -> {});
        assertEquals(0, narrow.bounds().get("demo:first").y());
        assertEquals(28, narrow.bounds().get("demo:last").y());

        var expandedFrames = List.of(frame(parent, "screen", 0, 0, 180, 120),
                frames.get(1), frames.get(2), frames.get(3));
        var wide = ContainerLayout.resolve(340, 200, expandedFrames,
                Map.of(parent, new PaddingElement()), Map.of(), parents,
                Map.of(parent, layout), Set.of("demo:middle"), Set.of(), warning -> {});
        assertEquals(0, wide.bounds().get("demo:last").y());

        draft.reset(parent);
        assertFalse(draft.hasSettingOverrides(parent));
        assertTrue(loaded.hasSettingOverrides(parent));
        assertFalse(saved.hasSettingOverrides(parent));
    }
}
