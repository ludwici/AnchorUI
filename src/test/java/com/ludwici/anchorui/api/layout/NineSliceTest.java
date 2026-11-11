package com.ludwici.anchorui.api.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NineSliceTest {
    @Test
    public void keepsCornerSizesWhenExpanding() {
        var tiles = NineSlice.tiles(200, 100, 64, 64, new NineSlice.Insets(8, 9, 10, 11));
        assertEquals(9, tiles.size());
        assertEquals(new NineSlice.Tile(0, 0, 8, 9, 0, 0, 8, 9), tiles.get(0));
        assertEquals(new NineSlice.Tile(8, 9, 182, 80, 8, 9, 46, 44), tiles.get(4));
        assertEquals(new NineSlice.Tile(190, 89, 10, 11, 54, 53, 10, 11), tiles.get(8));
    }

    @Test
    public void shrinksCornersToFitSmallTargets() {
        var tiles = NineSlice.tiles(9, 7, 30, 30, new NineSlice.Insets(7, 7, 7, 7));
        for (var tile : tiles) {
            assertTrue(tile.x() >= 0 && tile.y() >= 0);
            assertTrue(tile.x() + tile.width() <= 9);
            assertTrue(tile.y() + tile.height() <= 7);
        }
        assertEquals(9, tiles.size());
    }

    @Test
    public void rejectsSourceRegionsWithoutCenters() {
        assertThrows(IllegalArgumentException.class, () -> NineSlice.tiles(100, 100, 16, 16, new NineSlice.Insets(8, 8, 8, 0)));
        assertThrows(IllegalArgumentException.class, () -> new NineSlice.Insets(-1, 0, 0, 0));
    }
}
