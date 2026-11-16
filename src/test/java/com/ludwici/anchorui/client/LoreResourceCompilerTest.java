package com.ludwici.anchorui.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoreResourceCompilerTest {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("reload_test", path);
    }

    private static JsonElement json(String source) {
        return JsonParser.parseString(source);
    }

    @Test
    public void malformedResourcesDoNotPreventOtherScreensOrHudsFromLoading() {
        Map<ResourceLocation, JsonElement> resources = new LinkedHashMap<>();
        resources.put(id("style/valid"), json("""
                {"anchorui:text":{"text":{"color":"#FFFFFFFF"}}}
                """));
        resources.put(id("style/broken_option"), json("""
                {"anchorui:option":"not an object"}
                """));
        resources.put(id("widgets/valid"), json("""
                {"type":"anchorui:text","text":"Shared"}
                """));
        resources.put(id("widgets/broken"), json("""
                {"text":"Missing both type and ref"}
                """));
        resources.put(id("screens/good"), json("""
                {"imports":["reload_test:valid"],"entities":[{"type":"anchorui:text","id":"local"}]}
                """));
        resources.put(id("screens/bad"), json("""
                {"entities":[{"id":"missing_type"}]}
                """));
        resources.put(id("hud/good"), json("""
                {"testOnly":true,"testGroup":"rpg","entities":[{"type":"anchorui:progress","id":"health"}]}
                """));
        resources.put(id("hud/bad"), json("""
                {"entities":"not an array"}
                """));
        resources.put(id("screens/not_object"), json("""
                [1,2,3]
                """));

        LoreResourceCompiler.Snapshot compiled = LoreResourceCompiler.compile(resources);
        assertEquals(1, compiled.screens().size());
        assertEquals(2, compiled.screens().get(id("screens/good")).entities.size());
        assertEquals(1, compiled.huds().size());
        assertTrue(compiled.huds().get(id("hud/good")).testOnly());
        assertEquals("rpg", compiled.huds().get(id("hud/good")).testGroup());
        assertEquals(1, compiled.styles().size());
        assertTrue(compiled.styles().containsKey(id("style/valid")));
        assertEquals(1, compiled.definitions().ids().size());
        assertTrue(compiled.definitions().find(id("valid")).isPresent());
        assertEquals(5, compiled.issues().size());
        for (String path : List.of("style/broken_option", "widgets/broken", "screens/bad", "hud/bad", "screens/not_object")) {
            assertTrue(compiled.issues().stream().anyMatch(issue -> issue.resource().equals(id(path)) && issue.severity() == LoreResourceCompiler.Severity.ERROR), path);
        }
    }

    @Test
    public void malformedTemplateCannotOverrideAValidWidgetsDefinition() {
        Map<ResourceLocation, JsonElement> resources = new LinkedHashMap<>();
        resources.put(id("widgets/shared"), json("""
                {"type":"anchorui:text","text":"Valid fallback"}
                """));
        resources.put(id("templates/shared"), json("""
                {"type":17,"text":"Broken replacement"}
                """));
        resources.put(id("screens/consumer"), json("""
                {"imports":["reload_test:shared"]}
                """));
        LoreResourceCompiler.Snapshot first = LoreResourceCompiler.compile(resources);
        assertEquals(1, first.screens().get(id("screens/consumer")).entities.size());
        assertEquals("Valid fallback", first.screens().get(id("screens/consumer")).entities.getFirst().text);
        assertEquals(1, first.issues().size());
        assertEquals(id("templates/shared"), first.issues().getFirst().resource());

        resources.put(id("templates/shared"), json("""
                {"type":"anchorui:text","text":"Valid replacement"}
                """));
        LoreResourceCompiler.Snapshot second = LoreResourceCompiler.compile(resources);
        assertEquals("Valid replacement", second.screens().get(id("screens/consumer")).entities.getFirst().text);
        assertEquals(1, second.issues().size());
        assertEquals(LoreResourceCompiler.Severity.WARNING, second.issues().getFirst().severity());
    }

    @Test
    public void reloadBuildsFreshSnapshotsAndKeepsBrokenOptionalImportsIsolated() {
        Map<ResourceLocation, JsonElement> initial = new LinkedHashMap<>();
        initial.put(id("screens/good"), json("""
                {"imports":["reload_test:missing"],"entities":[{"type":"anchorui:text","id":"survivor"}]}
                """));
        LoreResourceCompiler.Snapshot first = LoreResourceCompiler.compile(initial);
        assertEquals(1, first.screens().get(id("screens/good")).entities.size());
        assertEquals(LoreResourceCompiler.Severity.WARNING, first.issues().getFirst().severity());
        assertTrue(first.issues().getFirst().message().contains("reload_test:missing"));
        assertTrue(first.issues().getFirst().message().contains("assets/reload_test/anchorui/widgets/missing.json"));

        Map<ResourceLocation, JsonElement> fixed = new LinkedHashMap<>(initial);
        fixed.put(id("widgets/missing"), json("""
                {"type":"anchorui:text","id":"header"}
                """));
        LoreResourceCompiler.Snapshot second = LoreResourceCompiler.compile(fixed);
        assertEquals(2, second.screens().get(id("screens/good")).entities.size());
        assertTrue(second.issues().isEmpty());
        assertFalse(first.issues().isEmpty());
        assertEquals(1, first.screens().get(id("screens/good")).entities.size());

        LoreResourceCompiler.Snapshot empty = LoreResourceCompiler.compile(Map.of());
        assertTrue(empty.screens().isEmpty());
        assertTrue(empty.huds().isEmpty());
        assertTrue(empty.styles().isEmpty());
        assertTrue(empty.definitions().ids().isEmpty());
        assertTrue(empty.issues().isEmpty());
    }
}
