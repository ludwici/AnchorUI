package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** A small shared navigation bar for HUD and Screen inspectors. */
public final class EditorInspectorTabs {
    public enum Page {
        POSITION("Place"),
        LAYOUT("Layout"),
        STYLE("Style"),
        MOTION("Motion"),
        MORE("More");

        private final String label;

        Page(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private static final int GAP = 2;
    private static final int HEIGHT = 18;
    private static final int ACTIVE = 0xFF41637A;
    private static final int IDLE = 0xFF253746;
    private static final int ACTIVE_TEXT = 0xFFE9F7FF;
    private static final int IDLE_TEXT = 0xFFAEBECD;
    private Page selected = Page.POSITION;

    public Page selected() {
        return selected;
    }

    public boolean active(Page page) {
        return selected == page;
    }

    public void select(Page page) {
        selected = java.util.Objects.requireNonNull(page);
    }

    public boolean click(double mouseX, double mouseY, int left, int top, int width) {
        if (mouseY < top || mouseY >= top + HEIGHT || mouseX < left || mouseX >= left + width) {
            return false;
        }
        int index = indexAt((int) mouseX, left, width);
        if (index < 0) {
            return false;
        }
        selected = Page.values()[index];
        return true;
    }

    public void render(GuiGraphics graphics, Font font, int left, int top, int width) {
        int count = Page.values().length;
        int itemWidth = Math.max(1, (width - GAP * (count - 1)) / count);
        for (int index = 0; index < count; index++) {
            int x = left + index * (itemWidth + GAP);
            Page page = Page.values()[index];
            boolean active = page == selected;
            graphics.fill(x, top, x + itemWidth, top + HEIGHT, active ? ACTIVE : IDLE);
            if (active) {
                graphics.fill(x, top + HEIGHT - 2, x + itemWidth, top + HEIGHT, 0xFF87D7D7);
            }
            graphics.drawCenteredString(font, font.plainSubstrByWidth(page.label(), itemWidth - 2), x + itemWidth / 2, top + 4, active ? ACTIVE_TEXT : IDLE_TEXT);
        }
    }

    public static int indexAt(int mouseX, int left, int width) {
        int count = Page.values().length;
        int itemWidth = Math.max(1, (width - GAP * (count - 1)) / count);
        for (int index = 0; index < count; index++) {
            int x = left + index * (itemWidth + GAP);
            if (mouseX >= x && mouseX < x + itemWidth) {
                return index;
            }
        }
        return -1;
    }

    public static int height() {
        return HEIGHT;
    }
}
