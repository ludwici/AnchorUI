package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ContainerScrollTest {
    @Test
    public void calculatesOverflowForBothAxes() {
        var bounds = Map.of("demo:container/__content", new AnchorLayout.Bounds(12, 14, 100, 60),
                "demo:child", new AnchorLayout.Bounds(42, 50, 200, 140));
        var limits = ContainerScroll.measure(bounds, Map.of("demo:child", "demo:container"),
                Map.of("demo:container", new ContainerScroll.Config(true, true, 18)));
        assertEquals(new ContainerScroll.Limits(130, 116), limits.get("demo:container"));
    }

    @Test
    public void clampsWheelScrollingAndAllowsReturnToTop() {
        var config = new ContainerScroll.Config(false, true, 20);
        var limits = new ContainerScroll.Limits(0, 75);
        assertEquals(new ContainerScroll.Offset(0, 20),
                ContainerScroll.move(ContainerScroll.Offset.ZERO, limits, config, 0, -1));
        assertEquals(new ContainerScroll.Offset(0, 75),
                ContainerScroll.move(new ContainerScroll.Offset(0, 65), limits, config, 0, -2));
        assertEquals(ContainerScroll.Offset.ZERO,
                ContainerScroll.move(new ContainerScroll.Offset(0, 10), limits, config, 0, 2));
    }

    @Test
    public void parsesModesAndRejectsInvalidStep() {
        assertEquals(new ContainerScroll.Config(false, true, 18),
                ContainerScroll.Config.parse(JsonParser.parseString("{\"scroll\": \"vertical\"}").getAsJsonObject()));
        assertEquals(new ContainerScroll.Config(true, false, 12),
                ContainerScroll.Config.parse(JsonParser.parseString("{\"scroll\": {\"axis\": \"horizontal\", \"step\": 12}}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class,
                () -> ContainerScroll.Config.parse(JsonParser.parseString("{\"scroll\": {\"step\": 0}}").getAsJsonObject()));
    }
}
