package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonParser;
import static com.ludwici.anchorui.api.layout.ContainerScroll.measure;
import com.ludwici.anchorui.api.layout.ContainerScroll.Config;
import com.ludwici.anchorui.api.layout.ContainerScroll.Limits;
import com.ludwici.anchorui.api.layout.ContainerScroll.Offset;
import com.ludwici.anchorui.api.style.PaddingElement;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ContainerScrollTest {
    @Test
    public void calculatesOverflowForBothAxes() {
        var bounds = Map.of("demo:container", new AnchorLayout.Bounds(10, 12, 104, 64), "demo:child", new AnchorLayout.Bounds(42, 50, 200, 140));
        var limits = ContainerScroll.measure(bounds, Map.of("demo:child", "demo:container"), Map.of("demo:container", new ContainerScroll.Config(true, true, 18)), Map.of("demo:container", new PaddingElement(2, 2)));
        assertEquals(new ContainerScroll.Limits(130, 116), limits.get("demo:container"));
    }

    @Test
    public void collapsedHiddenChildDoesNotCreatePhantomOverflow() {
        String parent = "demo:panel";
        String hidden = "demo:hidden";
        Map<String, AnchorLayout.Bounds> bounds = Map.of(parent, new AnchorLayout.Bounds(10, 12, 104, 64), "demo:visible", new AnchorLayout.Bounds(16, 20, 40, 25), hidden, new AnchorLayout.Bounds(30, 98, 100, 80));
        Map<String, String> parents = Map.of("demo:visible", parent, hidden, parent);
        Map<String, Config> configs = Map.of(parent, new Config(true, true, 18));
        Map<String, PaddingElement> paddings = Map.of(parent, new PaddingElement(2, 2));

        Limits reserving = measure(bounds, parents, configs, paddings).get(parent);
        assertEquals(new Limits(18, 104), reserving);

        Limits collapsed = measure(bounds, parents, configs, paddings, Set.of(hidden)).get(parent);
        assertEquals(new Limits(0, 0), collapsed);
        assertEquals(Offset.ZERO, collapsed.clamp(new Offset(17, 45)));
        assertEquals(reserving, measure(bounds, parents, configs, paddings, Set.of()).get(parent));
    }

    @Test
    public void collapsingOneContainerDoesNotSuppressAnotherContainersChild() {
        var bounds = Map.of("demo:first", new AnchorLayout.Bounds(0, 0, 60, 30), "demo:second", new AnchorLayout.Bounds(100, 0, 60, 30), "demo:gone", new AnchorLayout.Bounds(5, 50, 40, 20), "demo:keep", new AnchorLayout.Bounds(105, 50, 40, 20));
        var parents = Map.of("demo:gone", "demo:first", "demo:keep", "demo:second");
        var configurations = Map.of("demo:first", new Config(false, true, 18), "demo:second", new Config(false, true, 18));
        var paddings = Map.of("demo:first", new PaddingElement(), "demo:second", new PaddingElement());
        var limits = measure(bounds, parents, configurations, paddings, Set.of("demo:gone"));
        assertEquals(new Limits(0, 0), limits.get("demo:first"));
        assertEquals(new Limits(0, 40), limits.get("demo:second"));
    }

    @Test
    public void clampsWheelScrollingAndAllowsReturnToTop() {
        var config = new ContainerScroll.Config(false, true, 20);
        var limits = new ContainerScroll.Limits(0, 75);
        assertEquals(new ContainerScroll.Offset(0, 20), ContainerScroll.move(ContainerScroll.Offset.ZERO, limits, config, 0, -1));
        assertEquals(new ContainerScroll.Offset(0, 75), ContainerScroll.move(new ContainerScroll.Offset(0, 65), limits, config, 0, -2));
        assertEquals(ContainerScroll.Offset.ZERO, ContainerScroll.move(new ContainerScroll.Offset(0, 10), limits, config, 0, 2));
    }

    @Test
    public void parsesModesAndRejectsInvalidStep() {
        assertEquals(new ContainerScroll.Config(false, true, 18), ContainerScroll.Config.parse(JsonParser.parseString("{\"scroll\": \"vertical\"}").getAsJsonObject()));
        assertEquals(new ContainerScroll.Config(true, false, 12), ContainerScroll.Config.parse(JsonParser.parseString("{\"scroll\": {\"axis\": \"horizontal\", \"step\": 12}}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () -> ContainerScroll.Config.parse(JsonParser.parseString("{\"scroll\": {\"step\": 0}}").getAsJsonObject()));
    }
}
