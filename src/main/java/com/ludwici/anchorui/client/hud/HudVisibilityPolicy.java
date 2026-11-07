package com.ludwici.anchorui.client.hud;

public final class HudVisibilityPolicy {
    private HudVisibilityPolicy() {
    }

    public static boolean shouldRender(boolean worldReady, boolean hideGui, boolean screenOpen, boolean chatOpen) {
        return worldReady && !hideGui && (!screenOpen || chatOpen);
    }
}
