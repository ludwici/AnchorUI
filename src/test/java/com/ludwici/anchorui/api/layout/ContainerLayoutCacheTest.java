package com.ludwici.anchorui.api.layout;

import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PaddingElement;
import com.ludwici.anchorui.api.style.PositionElement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ContainerLayoutCacheTest {
    private static StackLayout.Config row(int gap) {
        return StackLayout.Config.parse(JsonParser.parseString("""
                {"layout":{"type":"row","gap":%d,"collapseHidden":true}}
                """.formatted(gap)).getAsJsonObject());
    }

    private static GridLayout.Config grid(int columns) {
        return GridLayout.Config.parse(JsonParser.parseString("""
                {"layout":{"type":"grid","columns":%d,"gap":3,"collapseHidden":true}}
                """.formatted(columns)).getAsJsonObject());
    }

    private static AnchorLayout.Frame frame(String id, String relativeTo, int x, int y, int width, int height) {
        return new AnchorLayout.Frame(id, new PositionElement(ElementAnchor.TOPLEFT, ElementAnchor.TOPLEFT, relativeTo, x, y), width, height);
    }

    private static ContainerLayout.Result resolve(ContainerLayout.Cache cache, List<AnchorLayout.Frame> frames,
                                                  Map<String, ContainerAutoLayout> layouts, Set<String> hidden) {
        return ContainerLayout.resolve(500, 320, frames, Map.of("demo:row", new PaddingElement(4, 4, 4, 4), "demo:grid", new PaddingElement()),
                Map.of(), Map.of("demo:a", "demo:row", "demo:b", "demo:row", "demo:c", "demo:grid", "demo:d", "demo:grid"),
                layouts, Map.of(), hidden, Set.of(), cache, message -> {
                    throw new AssertionError(message);
                });
    }

    @Test
    public void reusesEquivalentBuiltInConfigurationsAndInvalidatesChangedInputs() {
        ContainerLayout.Cache cache = new ContainerLayout.Cache();
        List<StackLayout.Item> items = List.of(new StackLayout.Item("demo:a", 20, 10, 0, 0), new StackLayout.Item("demo:b", 30, 10, 0, 0));
        var first = cache.arrange("demo:row", row(4), 200, 40, items, false, false);
        assertSame(first, cache.arrange("demo:row", row(4), 200, 40, List.copyOf(items), false, false));

        var changedWidth = cache.arrange("demo:row", row(4), 201, 40, items, false, false);
        assertNotSame(first, changedWidth);
        var changedChild = cache.arrange("demo:row", row(4), 201, 40, List.of(new StackLayout.Item("demo:a", 21, 10, 0, 0), items.get(1)), false, false);
        assertNotSame(changedWidth, changedChild);
        var changedOrder = cache.arrange("demo:row", row(4), 201, 40, List.of(items.get(1), items.get(0)), false, false);
        assertNotSame(changedChild, changedOrder);
        var changedLayout = cache.arrange("demo:row", row(5), 201, 40, items, false, false);
        assertNotSame(changedOrder, changedLayout);
        assertNotSame(changedLayout, cache.arrange("demo:row", row(5), 201, 40, items, true, false));

        cache.retain(Set.of());
        var afterRemoval = cache.arrange("demo:row", row(5), 201, 40, items, true, false);
        assertNotSame(changedLayout, afterRemoval);
        assertSame(afterRemoval, cache.arrange("demo:row", row(5), 201, 40, items, true, false));
    }

    @Test
    public void neverCachesCustomJavaLayoutFactories() {
        ContainerLayout.Cache cache = new ContainerLayout.Cache();
        AtomicInteger calls = new AtomicInteger();
        ContainerAutoLayout custom = new ContainerAutoLayout() {
            @Override
            public boolean collapseHidden() {
                return false;
            }

            @Override
            public Arrangement arrange(int availableWidth, int availableHeight, List<StackLayout.Item> items, boolean autoWidth, boolean autoHeight) {
                calls.incrementAndGet();
                return new Arrangement(Map.of(), new ContainerLayout.Size(1, 1));
            }
        };
        cache.arrange("demo:custom", custom, 100, 100, List.of(), false, false);
        cache.arrange("demo:custom", custom, 100, 100, List.of(), false, false);
        assertEquals(2, calls.get());
    }

    @Test
    public void cachedIndependentContainersMatchFullLayoutAfterVisibilityAndSizeChanges() {
        ContainerLayout.Cache cache = new ContainerLayout.Cache();
        Map<String, ContainerAutoLayout> layouts = Map.of("demo:row", row(6), "demo:grid", grid(2));
        List<AnchorLayout.Frame> frames = List.of(
                frame("demo:row", "screen", 10, 12, 150, 45),
                frame("demo:grid", "screen", 170, 12, 180, 90),
                frame("demo:a", "demo:row", 0, 0, 20, 10),
                frame("demo:b", "demo:row", 0, 0, 30, 15),
                frame("demo:c", "demo:grid", 0, 0, 18, 18),
                frame("demo:d", "demo:grid", 0, 0, 24, 25));
        var initial = resolve(cache, frames, layouts, Set.of());
        assertEquals(resolve(null, frames, layouts, Set.of()), initial);
        assertEquals(initial, resolve(cache, frames, layouts, Set.of()));

        var hidden = resolve(cache, frames, layouts, Set.of("demo:b"));
        assertEquals(resolve(null, frames, layouts, Set.of("demo:b")), hidden);
        assertEquals(20, hidden.bounds().get("demo:a").width());

        List<AnchorLayout.Frame> resized = List.of(frames.get(0), frames.get(1), frames.get(2),
                frame("demo:b", "demo:row", 0, 0, 40, 15), frames.get(4), frames.get(5));
        assertEquals(resolve(null, resized, layouts, Set.of()), resolve(cache, resized, layouts, Set.of()));
        assertEquals(resolve(null, resized, layouts, Set.of("demo:d")), resolve(cache, resized, layouts, Set.of("demo:d")));
        cache.clear();
        assertEquals(resolve(null, frames, layouts, Set.of()), resolve(cache, frames, layouts, Set.of()));
    }
}
