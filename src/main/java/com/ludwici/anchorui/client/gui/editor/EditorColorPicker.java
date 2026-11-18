package com.ludwici.anchorui.client.gui.editor;

import com.ludwici.anchorui.client.gui.ColorPickerWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.Consumer;

public final class EditorColorPicker {
    private final ColorPickerWidget picker;
    private final Button apply;
    private final Button cancel;
    private final Runnable onCancel;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public EditorColorPicker(Screen screen, String label, String currentColor,
                             int screenWidth, int screenHeight,
                             Consumer<String> onApply, Runnable onCancel) {
        Objects.requireNonNull(onApply);
        this.onCancel = Objects.requireNonNull(onCancel);
        int pickerWidth = Math.max(240, Math.min(288, screenWidth - 24));
        int pickerHeight = Math.max(152, Math.min(206, screenHeight - 77));
        width = pickerWidth + 16;
        height = pickerHeight + 72;
        x = Math.max(0, (screenWidth - width) / 2);
        y = Math.max(0, (screenHeight - height) / 2);
        picker = ColorPickerWidget.forEditor(screen, label, currentColor, pickerWidth, pickerHeight);
        picker.setPosition(x + 8, y + 25);
        picker.setFocused(true);
        int half = (width - 26) / 2;
        apply = Button.builder(Component.literal("Apply"), button -> {
            String chosen = picker.applyDraft();
            if (chosen != null) {
                onApply.accept(chosen);
            }
        }).bounds(x + 8, y + 31 + pickerHeight, half, 20).build();
        cancel = Button.builder(Component.literal("Cancel"), button -> onCancel.run())
                .bounds(x + 18 + half, y + 31 + pickerHeight, half, 20).build();
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY,
                       float partialTick, int screenWidth, int screenHeight, String error) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 500.0F);
        try {
            graphics.fill(0, 0, screenWidth, screenHeight, 0x99000000);
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF718699);
            graphics.fill(x, y, x + width, y + height, 0xFF162332);
            graphics.drawString(font, "EDIT COLOR", x + 8, y + 8, 0xFFFFD16B, false);
            picker.render(graphics, mouseX, mouseY, partialTick);
            apply.render(graphics, mouseX, mouseY, partialTick);
            cancel.render(graphics, mouseX, mouseY, partialTick);
            if (error != null && !error.isEmpty()) {
                graphics.drawString(font, font.plainSubstrByWidth(error, width - 16), x + 8, y + height - 13, 0xFFFFA68A, false);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (apply.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (cancel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        picker.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        picker.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        picker.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onCancel.run();
            return true;
        }
        picker.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        picker.charTyped(codePoint, modifiers);
        return true;
    }
}
