package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Three-way exit confirmation rendered over the existing editor, without replacing its screen. */
public final class EditorExitConfirmation {
    private final int screenWidth;
    private final int screenHeight;
    private final int left;
    private final int top;
    private final int dialogWidth;
    private final Button discardButton;
    private final Button saveButton;
    private final Button cancelButton;
    private boolean open;

    public EditorExitConfirmation(int screenWidth, int screenHeight, Runnable discard, Runnable save) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        dialogWidth = Math.min(320, Math.max(180, screenWidth - 16));
        left = (screenWidth - dialogWidth) / 2;
        top = Math.max(4, (screenHeight - 100) / 2);
        int spacing = 5;
        int buttonWidth = (dialogWidth - 20 - spacing * 2) / 3;
        int buttonY = top + 69;
        discardButton = Button.builder(Component.literal("Discard"), button -> {
            open = false;
            discard.run();
        }).bounds(left + 5, buttonY, buttonWidth, 20).build();
        saveButton = Button.builder(Component.literal("Save & Exit"), button -> {
            open = false;
            save.run();
        }).bounds(left + 10 + buttonWidth, buttonY, buttonWidth, 20).build();
        cancelButton = Button.builder(Component.literal("Cancel"), button -> close()).bounds(left + 15 + buttonWidth * 2, buttonY, buttonWidth, 20).build();
    }

    public boolean isOpen() {
        return open;
    }

    public void show() {
        open = true;
    }

    public void close() {
        open = false;
    }

    public boolean mouseClicked(double x, double y, int button) {
        if (!open) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (discardButton.mouseClicked(x, y, button) || saveButton.mouseClicked(x, y, button) || cancelButton.mouseClicked(x, y, button)) {
                return true;
            }
        }
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!open) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
        }
        return true;
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        if (!open) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 800.0F);
        try {
            graphics.fill(0, 0, screenWidth, screenHeight, 0xC0000000);
            graphics.fill(left - 1, top - 1, left + dialogWidth + 1, top + 96, 0xFF718699);
            graphics.fill(left, top, left + dialogWidth, top + 95, 0xFF182534);
            graphics.drawCenteredString(font, "Unsaved changes", screenWidth / 2, top + 11, 0xFFFFD16B);
            graphics.drawCenteredString(font, "Are you sure you want to exit?", screenWidth / 2, top + 32, 0xFFEAF2F7);
            graphics.drawCenteredString(font, "Your changes have not been saved.", screenWidth / 2, top + 46, 0xFFAAC1D4);
            discardButton.render(graphics, mouseX, mouseY, partialTick);
            saveButton.render(graphics, mouseX, mouseY, partialTick);
            cancelButton.render(graphics, mouseX, mouseY, partialTick);
        } finally {
            graphics.pose().popPose();
        }
    }
}
