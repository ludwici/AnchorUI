package com.ludwici.anchorui.api.animation;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiAnimationTimelineTest {
    private static UiAnimationSpec spec(String json) {
        return UiAnimationSpec.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    public void openFadeSlideAndScaleCompleteWithoutChangingLayoutCoordinates() {
        UiAnimationSpec s = spec("""
                {
                  "animation": {
                    "open": {
                      "duration": 200,
                      "easing": "linear",
                      "opacity": [0, 1],
                      "x": [-20, 0],
                      "y": [10, 0],
                      "scale": [0.8, 1]
                    }
                  }
                }
                """);
        UiAnimationTimeline t = new UiAnimationTimeline(s);
        t.update(true, null, 1000000L);
        var first = t.sample(1000000L);
        assertEquals(0.0, first.opacity(), 0.00001);
        assertEquals(-20.0, first.x(), 0.00001);
        var half = t.sample(101000000L);
        assertEquals(0.5, half.opacity(), 0.00001);
        assertEquals(-10.0, half.x(), 0.00001);
        assertEquals(5.0, half.y(), 0.00001);
        assertEquals(0.9, half.scale(), 0.00001);
        assertEquals(1.0, t.sample(201000000L).opacity(), 0.00001);
        assertTrue(t.finished(201000000L));
        assertEquals(40.0, half.inverseX(27.0, 10), 0.00001);
    }

    @Test
    public void hidingPreservesVisualFrameButNeverRestoresVisibility() {
        UiAnimationTimeline t = new UiAnimationTimeline(spec("""
                {
                  "animation": {
                    "hide": {
                      "duration": 100,
                      "opacity": [1, 0],
                      "y": [0, 8]
                    }
                  }
                }
                """));
        t.update(true, null, 0L);
        t.update(false, null, 100000000L);
        assertTrue(t.renderWhenHidden(140000000L));
        assertEquals(0.6, t.sample(140000000L).opacity(), 0.00001);
        assertFalse(t.renderWhenHidden(200000000L));
        t.update(true, null, 210000000L);
        assertFalse(t.renderWhenHidden(210000000L));
    }

    @Test
    public void changeWatchOnlyTriggersOnChangedSnapshot() {
        var doc = JsonParser.parseString("""
                {
                  "animation": {
                    "watch": {"bind": "anchorui:ui.state", "path": "counter", "fallback": 0},
                    "change": {"duration": 100, "scale": [1.2, 1]}
                  }
                }
                """).getAsJsonObject();
        UiAnimationSpec s = UiAnimationSpec.parse(doc);
        UiAnimationTimeline t = new UiAnimationTimeline(s);
        t.update(true, JsonParser.parseString("0"), 0);
        assertEquals(1.0, t.sample(10000000L).scale(), 0.00001);
        t.update(true, JsonParser.parseString("1"), 20000000L);
        assertEquals(1.2, t.sample(20000000L).scale(), 0.00001);
        t.update(true, JsonParser.parseString("1"), 40000000L);
        assertEquals(1.1, t.sample(70000000L).scale(), 0.00001);
        t.update(true, JsonParser.parseString("2"), 70000000L);
        assertEquals(1.1, t.sample(70000000L).scale(), 0.00001);
    }

    @Test
    public void invalidConfigIsExplicitlyRejected() {
        for (String invalid : new String[]{
                "{\"animation\":{\"open\":{\"duration\":0}}}",
                "{\"animation\":{\"open\":{\"duration\":50,\"opacity\":[-1,1]}}}",
                "{\"animation\":{\"open\":{\"duration\":50,\"scale\":[0,1]}}}",
                "{\"animation\":{\"open\":{\"duration\":50,\"easing\":\"bounce\"}}}",
                "{\"animation\":{\"strange\":{\"duration\":50}}}",
                "{\"animation\":{\"change\":{\"duration\":50}}}"
        }) {
            assertThrows(IllegalArgumentException.class, () -> spec(invalid));
        }
    }

    @Test
    public void editorPresetsApplyPerTriggerAndNoneCanDisableInheritedAnimation() {
        UiAnimationSpec entry = spec("""
                {
                  "animation": {
                    "open": {"preset":"fade"},
                    "show": {"preset":"slideLeft","duration":400,"easing":"easeOut"},
                    "hide": {"preset":"slideUp"},
                    "close": {"preset":"zoom"}
                  }
                }
                """);
        assertEquals(300L, entry.track(UiAnimationSpec.Trigger.OPEN).durationMillis());
        assertEquals(0.0, entry.track(UiAnimationSpec.Trigger.OPEN).from().opacity(), 0.000001);
        assertEquals(-32.0, entry.track(UiAnimationSpec.Trigger.SHOW).from().x(), 0.000001);
        assertEquals(-24.0, entry.track(UiAnimationSpec.Trigger.HIDE).to().y(), 0.000001);
        assertEquals(0.82, entry.track(UiAnimationSpec.Trigger.CLOSE).to().scale(), 0.000001);

        var defaults = JsonParser.parseString("""
                {"open": {"duration":500,"opacity":[0,1]}}
                """).getAsJsonObject();
        var override = JsonParser.parseString("""
                {"animation":{"open":{"preset":"none"}}}
                """).getAsJsonObject();
        UiAnimationSpec disabled = UiAnimationSpec.parse(override, defaults);
        assertEquals(null, disabled.track(UiAnimationSpec.Trigger.OPEN));
        assertThrows(IllegalArgumentException.class, () -> spec("{\"animation\":{\"open\":{\"preset\":\"unknown\"}}}"));
    }

    @Test
    public void localAnimationOverridesScreenWideDefaultsByTrigger() {
        var root = JsonParser.parseString("""
                {
                  "open": {"duration": 200, "opacity": [0, 1]},
                  "close": {"duration": 100, "opacity": [1, 0]}
                }
                """).getAsJsonObject();
        var local = JsonParser.parseString("""
                {
                  "animation": {"open": {"duration": 30, "x": [-8, 0]}}
                }
                """).getAsJsonObject();
        UiAnimationSpec s = UiAnimationSpec.parse(local, root);
        assertEquals(30L, s.track(UiAnimationSpec.Trigger.OPEN).durationMillis());
        assertEquals(100L, s.track(UiAnimationSpec.Trigger.CLOSE).durationMillis());
    }
}
