package com.ludwici.anchorui.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Popup surfaces are rendered after normal widgets and receive input before
 * widgets underneath them, so a select menu is not clipped to its button.
 */
public interface UiPopupWidget {
    boolean isPopupOpen();

    boolean containsPopup(double mouseX, double mouseY);

    boolean mouseClickedPopup(double mouseX, double mouseY, int button);

    boolean mouseScrolledPopup(double mouseX, double mouseY, double scrollY);

    void renderPopup(GuiGraphics graphics, int mouseX, int mouseY);

    void closePopup();
}
