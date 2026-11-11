package com.ludwici.anchorui.api.layout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RoundedPanelGeometryTest {
    @Test
    public void combinesTallStraightSectionIntoSingleBand() {
        List<RoundedPanelGeometry.Band> bands = RoundedPanelGeometry.bands(280, 180, 8, 2);
        assertTrue(bands.size() < 24);
        assertEquals(180, bands.stream().mapToInt(RoundedPanelGeometry.Band::height).sum());
        assertEquals(0, bands.getFirst().top());
        assertEquals(180, bands.getLast().top() + bands.getLast().height());
        for (int i = 1; i < bands.size(); i++) {
            assertEquals(bands.get(i - 1).top() + bands.get(i - 1).height(), bands.get(i).top());
        }
        assertTrue(bands.stream().anyMatch(band -> band.height() > 100 && band.hasInterior()));
    }

    @Test
    public void preservesEdgeAndInteriorForSmallPanel() {
        List<RoundedPanelGeometry.Band> bands = RoundedPanelGeometry.bands(12, 10, 5, 2);
        assertEquals(10, bands.stream().mapToInt(RoundedPanelGeometry.Band::height).sum());
        assertTrue(bands.stream().anyMatch(band -> !band.hasInterior()));
        assertTrue(bands.stream().anyMatch(RoundedPanelGeometry.Band::hasInterior));
        for (RoundedPanelGeometry.Band band : bands) {
            assertTrue(band.outerInset() >= 0);
            assertTrue(band.innerInset() >= 0);
        }
    }

    @Test
    public void rejectsInvalidSizesAndNegativeRadii() {
        assertThrows(IllegalArgumentException.class, () -> RoundedPanelGeometry.bands(0, 50, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> RoundedPanelGeometry.bands(20, 20, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> RoundedPanelGeometry.bands(20, 20, 1, -1));
    }
}
