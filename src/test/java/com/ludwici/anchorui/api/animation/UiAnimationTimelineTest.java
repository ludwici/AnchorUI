package com.ludwici.anchorui.api.animation;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiAnimationTimelineTest {
    private static UiAnimationSpec spec(String json) {
        return UiAnimationSpec.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    public void combinesUnlimitedTracksAndWaitsForDelay() {
        UiAnimationSpec parsed = spec("""
                {
                  "animations": [
                    {"id":"fade","trigger":"open","duration":200,"from":{"opacity":0},"to":{"opacity":1}},
                    {"id":"slide","trigger":"open","duration":200,"delay":100,"from":{"x":-20},"to":{"x":0}},
                    {"id":"zoom","trigger":"open","duration":200,"from":{"scale":0.8},"to":{"scale":1}},
                    {"id":"shift","trigger":"open","duration":200,"from":{"y":10},"to":{"y":0}}
                  ]
                }
                """);
        assertEquals(4, parsed.tracks().size());
        UiAnimationTimeline timeline = new UiAnimationTimeline(parsed);
        timeline.update(true, Map.of(), 0L);
        assertEquals(0.0, timeline.sample(0L).opacity(), 0.00001);
        assertEquals(-20.0, timeline.sample(0L).x(), 0.00001);
        assertEquals(0.5, timeline.sample(100000000L).opacity(), 0.00001);
        assertEquals(-20.0, timeline.sample(100000000L).x(), 0.00001);
        assertEquals(-10.0, timeline.sample(200000000L).x(), 0.00001);
        assertEquals(0.9, timeline.sample(100000000L).scale(), 0.00001);
        assertTrue(timeline.finished(300000000L));
        assertEquals(1.0, timeline.sample(300000000L).opacity(), 0.00001);
        assertEquals(0.0, timeline.sample(300000000L).x(), 0.00001);
    }

    @Test
    public void delayDoesNotChangeOpacityUntilStarting() {
        UiAnimationTimeline timeline = new UiAnimationTimeline(spec("""
                {"animations":[{"id":"late","trigger":"open","delay":250,"duration":100,"preset":"fade"}]}
                """));
        timeline.update(true, Map.of(), 0L);
        assertEquals(0.0, timeline.sample(200000000L).opacity(), 0.00001);
        assertEquals(0.0, timeline.sample(250000000L).opacity(), 0.00001);
        assertEquals(0.5, timeline.sample(300000000L).opacity(), 0.00001);
        assertEquals(1.0, timeline.sample(350000000L).opacity(), 0.00001);
    }

    @Test
    public void hideWaitsForDelayedExitAndPreventsPersistentHiddenRendering() {
        UiAnimationTimeline timeline = new UiAnimationTimeline(spec("""
                {"animations":[{"id":"hide","trigger":"hide","delay":100,"duration":100,"preset":"fade"}]}
                """));
        timeline.update(true, Map.of(), 0L);
        timeline.update(false, Map.of(), 200000000L);
        assertTrue(timeline.renderWhenHidden(340000000L));
        assertEquals(0.6, timeline.sample(340000000L).opacity(), 0.00001);
        assertFalse(timeline.renderWhenHidden(400000000L));
        timeline.update(true, Map.of(), 410000000L);
        assertEquals(1.0, timeline.sample(410000000L).opacity(), 0.00001);
    }

    @Test
    public void changeTracksWatchIndependentDataAndDoNotCancelOtherTracks() {
        UiAnimationTimeline timeline = new UiAnimationTimeline(spec("""
                {"animations":[
                  {"id":"hp","trigger":"change","duration":100,"from":{"scale":1.2},"watch":{"bind":"demo:health"}},
                  {"id":"mana","trigger":"change","delay":40,"duration":100,"from":{"x":20},"watch":{"bind":"demo:mana"}}
                ]}
                """));
        timeline.update(true, Map.of("hp", JsonParser.parseString("20"), "mana", JsonParser.parseString("10")), 0L);
        timeline.update(true, Map.of("hp", JsonParser.parseString("15"), "mana", JsonParser.parseString("10")), 100000000L);
        assertEquals(1.2, timeline.sample(100000000L).scale(), 0.00001);
        timeline.update(true, Map.of("hp", JsonParser.parseString("15"), "mana", JsonParser.parseString("9")), 150000000L);
        assertEquals(1.1, timeline.sample(150000000L).scale(), 0.00001);
        assertEquals(0.0, timeline.sample(150000000L).x(), 0.00001);
        assertEquals(20.0, timeline.sample(190000000L).x(), 0.00001);
        assertEquals(1.0, timeline.sample(250000000L).scale(), 0.00001);
    }

    @Test
    public void retriggerUsesCurrentFrameRatherThanJumpingToFirstKeyframe() {
        UiAnimationTimeline timeline = new UiAnimationTimeline(spec("""
                {"animations":[{"id":"pulse","trigger":"change","duration":100,"from":{"scale":1.2},"watch":{"bind":"demo:health"}}]}
                """));
        timeline.update(true, Map.of("pulse", JsonParser.parseString("0")), 0L);
        timeline.update(true, Map.of("pulse", JsonParser.parseString("1")), 20000000L);
        assertEquals(1.1, timeline.sample(70000000L).scale(), 0.00001);
        timeline.update(true, Map.of("pulse", JsonParser.parseString("2")), 70000000L);
        assertEquals(1.1, timeline.sample(70000000L).scale(), 0.00001);
    }

    @Test
    public void closeDurationIncludesLongestDelay() {
        UiAnimationTimeline timeline = new UiAnimationTimeline(spec("""
                {"animations":[
                    {"id":"a","trigger":"close","delay":200,"duration":400,"preset":"fade"},
                    {"id":"b","trigger":"close","delay":100,"duration":1000,"preset":"zoom"}
                ]}
                """));
        assertEquals(1100L, timeline.closeDurationMillis());
    }

    @Test
    public void catchesMalformedListsAndNoLongerAcceptsLegacyAnimation() {
        for (String json : new String[]{
                "{\"animation\":{\"open\":{\"duration\":100}}}",
                "{\"animations\":{}}",
                "{\"animations\":[{\"id\":\"same\"},{\"id\":\"same\"}]}",
                "{\"animations\":[{\"id\":\"bad\",\"delay\":-1}]}",
                "{\"animations\":[{\"id\":\"bad\",\"delay\":60001}]}",
                "{\"animations\":[{\"id\":\"bad\",\"duration\":0}]}",
                "{\"animations\":[{\"id\":\"bad\",\"preset\":\"boing\"}]}",
                "{\"animations\":[{\"id\":\"bad\",\"trigger\":\"change\"}]}",
                "{\"animations\":[{\"id\":\"bad\",\"from\":{\"scale\":0}}]}"
        }) {
            assertThrows(IllegalArgumentException.class, () -> spec(json));
        }
    }

    @Test
    public void customNamespacedTriggerFiresOnRisingConditionOrIncreasingRevision() {
        var custom = net.minecraft.resources.ResourceLocation.parse("test:quest_completed");
        UiAnimationTimeline timeline = new UiAnimationTimeline(spec("""
                {
                  "animations": [
                    {"id":"flash","trigger":"test:quest_completed","duration":200,"preset":"zoom"},
                    {"id":"fade","trigger":"test:quest_completed","delay":30,"duration":100,"preset":"fade"}
                  ]
                }
                """));
        timeline.update(true, Map.of(), Map.of(custom, 0L), 0L);
        assertEquals(1.0, timeline.sample(100000000L).scale(), 0.00001);
        timeline.update(true, Map.of(), Map.of(custom, 1L), 100000000L);
        assertEquals(0.82, timeline.sample(100000000L).scale(), 0.00001);
        timeline.update(true, Map.of(), Map.of(custom, 1L), 150000000L);
        assertEquals(0.865, timeline.sample(150000000L).scale(), 0.00001);
        timeline.update(true, Map.of(), Map.of(custom, 0L), 350000000L);
        assertEquals(1.0, timeline.sample(350000000L).scale(), 0.00001);
        timeline.update(true, Map.of(), Map.of(custom, 1L), 400000000L);
        assertEquals(0.82, timeline.sample(400000000L).scale(), 0.00001);
        assertThrows(IllegalArgumentException.class, () -> spec("{\"animations\":[{\"id\":\"bad\",\"trigger\":\"quest_completed\"}]}"));
    }

    @Test
    public void localListOverridesDocumentDefaultsAndCanBeEmpty() {
        var defaults = JsonParser.parseString("""
                [{"id":"default","trigger":"open","preset":"fade"}]
                """).getAsJsonArray();
        var local = JsonParser.parseString("""
                {"animations":[{"id":"custom","trigger":"show","preset":"zoom"}]}
                """).getAsJsonObject();
        assertEquals("custom", UiAnimationSpec.parse(local, defaults).tracks().getFirst().id());
        assertEquals(0, UiAnimationSpec.parse(JsonParser.parseString("{\"animations\":[]}").getAsJsonObject(), defaults).tracks().size());
        assertEquals("default", UiAnimationSpec.parse(JsonParser.parseString("{}").getAsJsonObject(), defaults).tracks().getFirst().id());
    }
}
