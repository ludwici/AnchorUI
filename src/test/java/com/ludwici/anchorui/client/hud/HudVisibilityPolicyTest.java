package com.ludwici.anchorui.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HudVisibilityPolicyTest {
    @Test
    public void keepsHudVisibleWhenChatIsOpen() {
        assertTrue(HudVisibilityPolicy.shouldRender(true, false, false, false));
        assertTrue(HudVisibilityPolicy.shouldRender(true, false, true, true));
    }

    @Test
    public void hidesHudForMenusF1AndMissingWorld() {
        assertFalse(HudVisibilityPolicy.shouldRender(true, false, true, false));
        assertFalse(HudVisibilityPolicy.shouldRender(true, true, false, false));
        assertFalse(HudVisibilityPolicy.shouldRender(true, true, true, true));
        assertFalse(HudVisibilityPolicy.shouldRender(false, false, true, true));
    }
}
