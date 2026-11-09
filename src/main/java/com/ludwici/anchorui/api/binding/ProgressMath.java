package com.ludwici.anchorui.api.binding;

public final class ProgressMath {
    private ProgressMath() {
    }

    public static double fraction(double value, double maximum) {
        if (!Double.isFinite(value) || !Double.isFinite(maximum) || maximum <= 0) {
            return 0;
        }
        return Math.max(0.0, Math.min(1.0, value / maximum));
    }
}
