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
    public void batchedBandsMatchPerRowRoundedGeometry() {
        for (int width : new int[]{7, 12, 50, 280}) {
            for (int height : new int[]{7, 10, 40, 180}) {
                for (int radius : new int[]{0, 2, 8, 20}) {
                    for (int border : new int[]{0, 1, 3}) {
                        List<RoundedPanelGeometry.Band> bands = RoundedPanelGeometry.bands(width, height, radius, border);
                        int actualBorder = Math.min(border, Math.min(width, height) / 2);
                        int outerRadius = Math.min(radius, Math.min(width, height) / 2);
                        int innerWidth = width - 2 * actualBorder;
                        int innerHeight = height - 2 * actualBorder;
                        int innerRadius = Math.max(0, outerRadius - actualBorder);
                        innerRadius = Math.min(innerRadius, Math.min(innerWidth, innerHeight) / 2);
                        int row = 0;
                        for (RoundedPanelGeometry.Band band : bands) {
                            assertEquals(row, band.top());
                            for (int i = 0; i < band.height(); i++) {
                                int innerRow = row - actualBorder;
                                boolean inside = innerWidth > 0 && innerHeight > 0
                                        && innerRow >= 0 && innerRow < innerHeight;
                                assertEquals(inset(row, height, outerRadius), band.outerInset());
                                assertEquals(inside, band.hasInterior());
                                assertEquals(inside ? inset(innerRow, innerHeight, innerRadius) : 0, band.innerInset());
                                row++;
                            }
                        }
                        assertEquals(height, row);
                    }
                }
            }
        }
    }

    private static int inset(int row, int height, int radius) {
        if (radius <= 0) {
            return 0;
        }
        double distance = Math.max(radius - row - 0.5, row + 0.5 - (height - radius));
        if (distance <= 0) {
            return 0;
        }
        return (int) Math.ceil(radius - Math.sqrt(Math.max(0, radius * radius - distance * distance)));
    }

    @Test
    public void rejectsInvalidSizesAndNegativeRadii() {
        assertThrows(IllegalArgumentException.class, () -> RoundedPanelGeometry.bands(0, 50, 5, 1));
        assertThrows(IllegalArgumentException.class, () -> RoundedPanelGeometry.bands(20, 20, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> RoundedPanelGeometry.bands(20, 20, 1, -1));
    }
}
