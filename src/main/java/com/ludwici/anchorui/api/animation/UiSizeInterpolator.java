package com.ludwici.anchorui.api.animation;

import com.ludwici.anchorui.api.animation.UiAnimationSpec.Easing;

/**
 * Interpolates a normalized rendered dimension independently of the layout.
 * Retargeting samples the current displayed value rather than restarting.
 */
public final class UiSizeInterpolator {
    private final long durationNanos;
    private final Easing easing;
    private boolean initialized;
    private double from;
    private double target;
    private long startedAt;

    public UiSizeInterpolator(long durationMillis, Easing easing) {
        if (durationMillis < 1 || durationMillis > 10000 || easing == null) {
            throw new IllegalArgumentException("Size transition duration must be 1..10000ms with valid easing");
        }
        durationNanos = durationMillis * 1000000L;
        this.easing = easing;
    }

    public double sample(double fraction, long nowNanos) {
        double desired = normalized(fraction);
        if (!initialized) {
            initialized = true;
            from = desired;
            target = desired;
            startedAt = nowNanos;
            return desired;
        }
        double current = current(nowNanos);
        if (Math.abs(desired - target) > 0.000001) {
            from = current;
            target = desired;
            startedAt = nowNanos;
            return current;
        }
        return current;
    }

    private double current(long nowNanos) {
        double elapsed = Math.max(0.0, (nowNanos - startedAt) / (double) durationNanos);
        double progress = easing.apply(elapsed);
        return from + (target - from) * progress;
    }

    private static double normalized(double fraction) {
        if (!Double.isFinite(fraction)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, fraction));
    }
}
