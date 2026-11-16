package com.ludwici.anchorui.api.animation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UiSizeInterpolatorTest {
    @Test
    public void startsAtRealValueAndAnimatesBothDirections() {
        UiSizeInterpolator transition = new UiSizeInterpolator(400, UiAnimationSpec.Easing.LINEAR);
        assertEquals(1.0, transition.sample(1, 0L), 0.000001);
        assertEquals(1.0, transition.sample(0.25, 100000000L), 0.000001);
        assertEquals(0.625, transition.sample(0.25, 300000000L), 0.000001);
        assertEquals(0.25, transition.sample(0.25, 500000000L), 0.000001);
        assertEquals(0.25, transition.sample(1, 600000000L), 0.000001);
        assertEquals(0.625, transition.sample(1, 800000000L), 0.000001);
        assertEquals(1.0, transition.sample(1, 1000000000L), 0.000001);
    }

    @Test
    public void rapidTargetChangesContinueFromVisibleWidth() {
        UiSizeInterpolator transition = new UiSizeInterpolator(200, UiAnimationSpec.Easing.LINEAR);
        transition.sample(1, 0);
        transition.sample(0, 100000000L);
        assertEquals(0.5, transition.sample(0, 200000000L), 0.000001);
        assertEquals(0.5, transition.sample(1, 200000000L), 0.000001);
        assertEquals(0.75, transition.sample(1, 300000000L), 0.000001);
    }

    @Test
    public void invalidAndOutOfRangeTargetsAreClamped() {
        UiSizeInterpolator transition = new UiSizeInterpolator(100, UiAnimationSpec.Easing.LINEAR);
        assertEquals(0.0, transition.sample(Double.NaN, 0L), 0.000001);
        transition.sample(2.0, 100000000L);
        assertEquals(1.0, transition.sample(2.0, 200000000L), 0.000001);
    }
}
