package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared modal choice popup for HUD and screen inspectors.
 * It is rendered above the editor controls and receives input before them.
 */
public final class EditorDropdown {
    private static final int ROW_HEIGHT = 18;
    private static final int BORDER = 0xFF718699;
    private static final int BACKGROUND = 0xFF192736;
    private static final int HOVER = 0xFF324E66;
    private static final int SELECTED = 0xFF386A86;
    private static final int TEXT = 0xFFE6F0F5;
    private static final int MUTED = 0xFF9EB6C5;

    private List<String> options = List.of();
    private Consumer<String> listener;
    private int x;
    private int y;
    private int width;
    private int visibleRows;
    private int scrollStart;
    private int highlighted;

    public boolean isOpen() {
        return listener != null;
    }

    public void close() {
        listener = null;
        options = List.of();
    }

    public void open(int originX, int originY, int originWidth, int originHeight,
                     int screenWidth, int screenHeight, List<String> choices,
                     String current, Consumer<String> action) {
        Objects.requireNonNull(choices, "Dropdown choices are required");
        Objects.requireNonNull(action, "Dropdown selection action is required");
        if (choices.isEmpty()) {
            throw new IllegalArgumentException("Dropdown requires at least one option");
        }
        options = List.copyOf(choices);
        listener = action;
        width = Math.min(Math.max(1, screenWidth - 4), Math.max(130, originWidth));
        x = Math.max(2, Math.min(originX, screenWidth - width - 2));
        visibleRows = Math.min(options.size(), Math.max(1, (screenHeight - 12) / ROW_HEIGHT));
        int popupHeight = visibleRows * ROW_HEIGHT + 2;
        int below = originY + originHeight;
        int above = originY - popupHeight;
        y = below + popupHeight <= screenHeight - 2 ? below : above >= 2 ? above
                : Math.max(2, Math.min(below, screenHeight - popupHeight - 2));
        highlighted = Math.max(0, options.indexOf(current));
        scrollStart = Math.max(0, Math.min(highlighted - visibleRows + 1, options.size() - visibleRows));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isOpen()) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && contains(mouseX, mouseY)) {
            int index = scrollStart + Math.min(visibleRows - 1, Math.max(0, ((int) mouseY - y - 1) / ROW_HEIGHT));
            choose(index);
        } else {
            close();
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!isOpen()) {
            return false;
        }
        if (contains(mouseX, mouseY)) {
            int next = scrollStart - (int) Math.signum(scrollY);
            scrollStart = Math.max(0, Math.min(options.size() - visibleRows, next));
        }
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!isOpen()) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
        } else if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            highlighted = Math.max(0, Math.min(options.size() - 1,
                    highlighted + (keyCode == GLFW.GLFW_KEY_UP ? -1 : 1)));
            scrollStart = Math.max(0, Math.min(scrollStart, highlighted));
            scrollStart = Math.max(scrollStart, highlighted - visibleRows + 1);
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE) {
            choose(highlighted);
        }
        return true;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (!isOpen()) {
            return;
        }
        int height = visibleRows * ROW_HEIGHT + 2;
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 600);
        try {
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BORDER);
            graphics.fill(x, y, x + width, y + height, BACKGROUND);
            for (int row = 0; row < visibleRows; row++) {
                int index = scrollStart + row;
                int rowY = y + 1 + row * ROW_HEIGHT;
                boolean hovering = mouseX >= x && mouseX < x + width
                        && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                if (index == highlighted || hovering) {
                    graphics.fill(x + 1, rowY, x + width - 1, rowY + ROW_HEIGHT,
                            hovering ? HOVER : SELECTED);
                }
                String text = font.plainSubstrByWidth(options.get(index), width - 18);
                graphics.drawString(font, text, x + 7, rowY + (ROW_HEIGHT - font.lineHeight) / 2,
                        index == highlighted ? TEXT : MUTED, false);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y
                && mouseY < y + visibleRows * ROW_HEIGHT + 2;
    }

    private void choose(int index) {
        Consumer<String> action = listener;
        String value = options.get(index);
        close();
        action.accept(value);
    }
}
