package com.ludwici.anchorui.api.binding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProgressMathTest {
    @Test
    public void computesFractionsAndClampsAtBothEnds() {
        assertEquals(0.5, ProgressMath.fraction(10, 20));
        assertEquals(0, ProgressMath.fraction(-5, 20));
        assertEquals(1, ProgressMath.fraction(35, 20));
        assertEquals(0, ProgressMath.fraction(1, 0));
        assertEquals(0, ProgressMath.fraction(1, -1));
        assertEquals(0, ProgressMath.fraction(Double.NaN, 20));
        assertEquals(0, ProgressMath.fraction(10, Double.POSITIVE_INFINITY));
    }
}
