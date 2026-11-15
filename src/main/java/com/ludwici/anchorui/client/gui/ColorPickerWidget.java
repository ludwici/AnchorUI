package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.state.UiLocalState;
import com.ludwici.anchorui.api.state.UiStateHost;
import com.ludwici.anchorui.api.style.LoreStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import com.ludwici.anchorui.api.widget.UiChangeSource;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.Consumer;

public final class ColorPickerWidget extends AbstractWidget implements UiChangeSource {
    private static final int MIN_WIDTH = 240;
    private static final int MIN_HEIGHT = 152;
    private static final int DEFAULT_WIDTH = 288;
    private static final int DEFAULT_HEIGHT = 166;
    private static final int CELL_SIZE = 4;
    private enum DragTarget {
        NONE, PALETTE, HUE
    }

    private record Geometry(int paletteX, int paletteY, int paletteWidth, int paletteHeight,
                            int hueX, int fieldsX, int fieldsWidth) {
    }

    private final UiLocalState state;
    private final String stateKey;
    private final WidgetContext context;
    private final LoreStyle.Element style;
    private final String label;
    private final EditBox hexInput;
    private final EditBox rgbInput;
    private final int backgroundColor;
    private final int borderColor;
    private Consumer<JsonElement> changeListener = ignored -> {};
    private DragTarget dragging = DragTarget.NONE;
    private int color;
    private float hue;
    private float saturation;
    private float brightness;
    private String invalidInput = "";

    private ColorPickerWidget(WidgetContext context, String label, UiLocalState state,
                              String stateKey, LoreStyle.Element style) {
        super(0, 0, style.size.width > 0 ? style.size.width : DEFAULT_WIDTH, style.size.height > 0 ? style.size.height : DEFAULT_HEIGHT, Component.literal(label));
        if (getWidth() < MIN_WIDTH || getHeight() < MIN_HEIGHT) {
            throw new IllegalArgumentException("color_picker requires at least " + MIN_WIDTH + "x" + MIN_HEIGHT + " pixels");
        }
        this.context = context;
        this.label = label;
        this.state = state;
        this.stateKey = stateKey;
        this.style = style;
        JsonObject settings = style.resolvedSettings;
        backgroundColor = WidgetColors.parse(GsonHelper.getAsString(settings, "fillColor", "#D923354A"));
        borderColor = WidgetColors.parse(GsonHelper.getAsString(settings, "borderColor", "#FF718699"));
        color = ColorPickerColor.parse(state.get(stateKey).getAsString());
        setHsvFromColor();
        hexInput = new EditBox(context.font(), 0, 0, 90, 18, Component.literal("HEX"));
        hexInput.setMaxLength(9);
        rgbInput = new EditBox(context.font(), 0, 0, 90, 18, Component.literal("RGB"));
        rgbInput.setMaxLength(32);
        updateInputValues();
    }

    public static ColorPickerWidget create(WidgetContext context, LoreEntity.UiElement element, LoreStyle.Element style) {
        if (!(context.screen() instanceof UiStateHost host)) {
            throw new IllegalArgumentException("color_picker requires an AnchorUI screen with Local UI State");
        }
        String key = GsonHelper.getAsString(element.settings, "stateKey", "");
        if (key.isBlank()) {
            throw new IllegalArgumentException("color_picker requires 'stateKey'");
        }
        JsonElement initial = host.uiState().get(key);
        if (!initial.isJsonPrimitive() || !initial.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("color_picker requires a string UI state key: " + key);
        }
        ColorPickerColor.parse(initial.getAsString());
        return new ColorPickerWidget(context, element.text, host.uiState(), key, style);
    }

    public static ColorPickerWidget forEditor(Screen editor, String label, String value, int width, int height) {
        JsonObject initialState = new JsonObject();
        initialState.addProperty("draftColor", ColorPickerColor.hex(ColorPickerColor.parse(value)));
        JsonObject settings = new JsonObject();
        JsonObject size = new JsonObject();
        size.addProperty("width", width);
        size.addProperty("height", height);
        settings.add("size", size);
        WidgetContext context = new WidgetContext(Minecraft.getInstance(), editor, ItemStack.EMPTY, new LoreEntity(new JsonObject()));
        return new ColorPickerWidget(context, label, new UiLocalState(initialState), "draftColor", new LoreStyle.Element(settings));
    }

    public String applyDraft() {
        if (hexInput.isFocused()) {
            commitInput(hexInput);
        }
        if (rgbInput.isFocused()) {
            commitInput(rgbInput);
        }
        if (!invalidInput.isEmpty()) {
            return null;
        }
        return ColorPickerColor.hex(color);
    }

    @Override
    public void setChangeListener(Consumer<JsonElement> listener) {
        changeListener = Objects.requireNonNull(listener, "Color picker change listener cannot be null");
    }

    private void setHsvFromColor() {
        float[] channels = ColorPickerColor.hsv(color);
        hue = channels[0];
        saturation = channels[1];
        brightness = channels[2];
    }

    private void updateInputValues() {
        if (!hexInput.isFocused()) {
            hexInput.setValue(ColorPickerColor.hex(color));
        }
        if (!rgbInput.isFocused()) {
            rgbInput.setValue(ColorPickerColor.rgb(color));
        }
    }

    private void refreshFromState() {
        int current = ColorPickerColor.parse(state.get(stateKey).getAsString());
        if (current != color) {
            color = current;
            setHsvFromColor();
            updateInputValues();
        }
    }

    private void setColor(int nextColor) {
        if (nextColor == color) {
            return;
        }
        color = nextColor;
        state.set(stateKey, new JsonPrimitive(ColorPickerColor.hex(color)));
        invalidInput = "";
        updateInputValues();
        changeListener.accept(new JsonPrimitive(ColorPickerColor.hex(color)));
    }

    private Geometry geometry() {
        int x = getX();
        int y = getY();
        int paletteWidth = Math.min(112, Math.max(80, getWidth() - 157));
        int paletteHeight = Math.min(112, Math.max(80, getHeight() - 52));
        int paletteX = x + 8;
        int paletteY = y + 24;
        int hueX = paletteX + paletteWidth + 6;
        int fieldsX = hueX + 20;
        return new Geometry(paletteX, paletteY, paletteWidth, paletteHeight, hueX, fieldsX, Math.max(50, getRight() - fieldsX - 8));
    }

    private void positionInputs(Geometry g) {
        hexInput.setPosition(g.fieldsX(), getY() + 70);
        hexInput.setWidth(g.fieldsWidth());
        rgbInput.setPosition(g.fieldsX(), getY() + 109);
        rgbInput.setWidth(g.fieldsWidth());
    }

    private void applyPalette(double mouseX, double mouseY, Geometry g) {
        saturation = clamp((float) ((mouseX - g.paletteX()) / (g.paletteWidth() - 1)));
        brightness = 1.0F - clamp((float) ((mouseY - g.paletteY()) / (g.paletteHeight() - 1)));
        setColor(ColorPickerColor.hsvColor(hue, saturation, brightness, color >>> 24));
    }

    private void applyHue(double mouseY, Geometry g) {
        hue = clamp((float) ((mouseY - g.paletteY()) / (g.paletteHeight() - 1)));
        setColor(ColorPickerColor.hsvColor(hue, saturation, brightness, color >>> 24));
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static boolean within(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void commitInput(EditBox input) {
        if (!input.isFocused()) {
            return;
        }
        try {
            int next = input == hexInput ? ColorPickerColor.parseHex(input.getValue())
                    : ColorPickerColor.parseRgb(input.getValue(), color >>> 24);
            input.setFocused(false);
            setColor(next);
            invalidInput = "";
            updateInputValues();
        } catch (IllegalArgumentException exception) {
            invalidInput = exception.getMessage();
        }
    }

    private void finishEditing() {
        if (hexInput.isFocused()) {
            commitInput(hexInput);
        }
        if (rgbInput.isFocused()) {
            commitInput(rgbInput);
        }
        hexInput.setFocused(false);
        rgbInput.setFocused(false);
        updateInputValues();
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused) {
            finishEditing();
            dragging = DragTarget.NONE;
        }
        super.setFocused(focused);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshFromState();
        Geometry g = geometry();
        positionInputs(g);
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        graphics.fill(x, y, x + width, y + height, borderColor);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, backgroundColor);
        String heading = label.isBlank() ? "Color picker" : label;
        graphics.drawString(context.font(), context.font().plainSubstrByWidth(heading, width - 16), x + 8, y + 7, style.textColor, false);

        for (int py = 0; py < g.paletteHeight(); py += CELL_SIZE) {
            for (int px = 0; px < g.paletteWidth(); px += CELL_SIZE) {
                float saturationAt = (float) px / Math.max(1, g.paletteWidth() - 1);
                float brightnessAt = 1.0F - (float) py / Math.max(1, g.paletteHeight() - 1);
                int shade = ColorPickerColor.hsvColor(hue, saturationAt, brightnessAt, 255);
                graphics.fill(g.paletteX() + px, g.paletteY() + py, g.paletteX() + Math.min(g.paletteWidth(), px + CELL_SIZE), g.paletteY() + Math.min(g.paletteHeight(), py + CELL_SIZE), shade);
            }
        }
        int markerX = g.paletteX() + Math.round(saturation * (g.paletteWidth() - 1));
        int markerY = g.paletteY() + Math.round((1.0F - brightness) * (g.paletteHeight() - 1));
        outline(graphics, markerX - 3, markerY - 3, 7, 7, 0xFF000000);
        outline(graphics, markerX - 2, markerY - 2, 5, 5, 0xFFFFFFFF);
        for (int py = 0; py < g.paletteHeight(); py += 2) {
            float hueAt = (float) py / Math.max(1, g.paletteHeight() - 1);
            int shade = ColorPickerColor.hsvColor(hueAt, 1.0F, 1.0F, 255);
            graphics.fill(g.hueX(), g.paletteY() + py, g.hueX() + 12, g.paletteY() + Math.min(py + 2, g.paletteHeight()), shade);
        }
        int hueMarkerY = g.paletteY() + Math.round(hue * (g.paletteHeight() - 1));
        graphics.fill(g.hueX() - 1, hueMarkerY - 1, g.hueX() + 13, hueMarkerY + 2, 0xFF000000);
        graphics.fill(g.hueX(), hueMarkerY, g.hueX() + 12, hueMarkerY + 1, 0xFFFFFFFF);

        for (int previewY = y + 25; previewY < y + 53; previewY += 7) {
            for (int previewX = g.fieldsX(); previewX < g.fieldsX() + g.fieldsWidth(); previewX += 7) {
                int base = (((previewX - g.fieldsX()) / 7 + (previewY - y - 25) / 7) & 1) == 0
                        ? 0xFF999999 : 0xFF555555;
                graphics.fill(previewX, previewY, Math.min(previewX + 7, g.fieldsX() + g.fieldsWidth()), Math.min(previewY + 7, y + 53), base);
            }
        }
        graphics.fill(g.fieldsX(), y + 25, g.fieldsX() + g.fieldsWidth(), y + 53, color);
        graphics.drawString(context.font(), "HEX", g.fieldsX(), y + 59, style.textColor, false);
        graphics.drawString(context.font(), "RGB", g.fieldsX(), y + 98, style.textColor, false);
        hexInput.render(graphics, mouseX, mouseY, partialTick);
        rgbInput.render(graphics, mouseX, mouseY, partialTick);
        String hint = invalidInput.isEmpty() ? "Enter to apply | Drag to pick" : invalidInput;
        graphics.drawString(context.font(), context.font().plainSubstrByWidth(hint, width - 16), x + 8, y + height - 15, invalidInput.isEmpty() ? style.textColor : 0xFFFF7777, false);
        if (isFocused() && !hexInput.isFocused() && !rgbInput.isFocused()) {
            outline(graphics, x, y, width, height, 0xFFFFD16B);
        }
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int argb) {
        if (width < 2 || height < 2) {
            return;
        }
        graphics.fill(x, y, x + width, y + 1, argb);
        graphics.fill(x, y + height - 1, x + width, y + height, argb);
        graphics.fill(x, y + 1, x + 1, y + height - 1, argb);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!active || !visible || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        refreshFromState();
        Geometry g = geometry();
        positionInputs(g);
        if (hexInput.isMouseOver(mouseX, mouseY)) {
            if (rgbInput.isFocused()) {
                commitInput(rgbInput);
                rgbInput.setFocused(false);
            }
            hexInput.setFocused(true);
            hexInput.mouseClicked(mouseX, mouseY, button);
            dragging = DragTarget.NONE;
            return true;
        }
        if (rgbInput.isMouseOver(mouseX, mouseY)) {
            if (hexInput.isFocused()) {
                commitInput(hexInput);
                hexInput.setFocused(false);
            }
            rgbInput.setFocused(true);
            rgbInput.mouseClicked(mouseX, mouseY, button);
            dragging = DragTarget.NONE;
            return true;
        }
        finishEditing();
        if (within(mouseX, mouseY, g.paletteX(), g.paletteY(), g.paletteWidth(), g.paletteHeight())) {
            dragging = DragTarget.PALETTE;
            applyPalette(mouseX, mouseY, g);
        } else if (within(mouseX, mouseY, g.hueX(), g.paletteY(), 12, g.paletteHeight())) {
            dragging = DragTarget.HUE;
            applyHue(mouseY, g);
        } else {
            dragging = DragTarget.NONE;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!active || dragging == DragTarget.NONE || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        Geometry g = geometry();
        if (dragging == DragTarget.PALETTE) {
            applyPalette(mouseX, mouseY, g);
        } else {
            applyHue(mouseY, g);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && dragging != DragTarget.NONE) {
            dragging = DragTarget.NONE;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (hexInput.isFocused()) {
                commitInput(hexInput);
                return true;
            }
            if (rgbInput.isFocused()) {
                commitInput(rgbInput);
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_TAB && hexInput.isFocused()) {
            commitInput(hexInput);
            hexInput.setFocused(false);
            rgbInput.setFocused(true);
            return true;
        }
        if (hexInput.isFocused()) {
            return hexInput.keyPressed(keyCode, scanCode, modifiers);
        }
        if (rgbInput.isFocused()) {
            return rgbInput.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!active || !visible) {
            return false;
        }
        if (hexInput.isFocused()) {
            return hexInput.charTyped(codePoint, modifiers);
        }
        return rgbInput.isFocused() && rgbInput.charTyped(codePoint, modifiers);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal((label.isBlank() ? "Color picker" : label) + ": " + ColorPickerColor.hex(color)));
    }
}
