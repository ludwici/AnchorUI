package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.state.UiControlOptions;
import com.ludwici.anchorui.api.state.UiLocalState;
import com.ludwici.anchorui.api.state.UiStateHost;
import com.ludwici.anchorui.api.style.LoreStyle;
import com.ludwici.anchorui.api.widget.UiChangeSource;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Built-in stateful controls. Every change updates the owning screen's local
 * UI state and notifies the ordinary AnchorUI onChange event pipeline.
 */
public final class StateControlWidget extends AbstractButton implements UiChangeSource, UiPopupWidget {
    public enum Kind {
        CHECKBOX, SWITCH, SELECT, TABS
    }

    private static final int BORDER = 0xFF718699;
    private static final int BASE = 0xD923354A;
    private static final int SELECTED = 0xFF426D94;
    private static final int HOVER = 0xFF36506A;
    private static final int TEXT = 0xFFF1F4F7;
    private static final int MUTED = 0xFFB6C5D1;
    private static final int POPUP_ROW_HEIGHT = 20;
    private static final int MAX_VISIBLE_OPTIONS = 8;

    private final WidgetContext context;
    private final Kind kind;
    private final UiLocalState state;
    private final String key;
    private final String label;
    private final List<UiControlOptions.Choice> options;
    private final int textColor;
    private final int fillColor;
    private final int borderColor;
    private final int accentColor;
    private final int hoverColor;
    private final int disabledColor;
    private final int focusColor;
    private final StateControlTextures textures;
    private final List<SelectOptionAppearance> optionAppearances;
    private final int indicatorSize;
    private final int trackWidth;
    private final int trackHeight;
    private final int thumbWidth;
    private final int thumbHeight;
    private final int arrowSize;
    private final int popupRowHeight;
    private Consumer<JsonElement> changeListener = ignored -> {};
    private boolean popupOpen;
    private int scrollStart;
    private int highlight;

    private StateControlWidget(WidgetContext context, Kind kind, UiLocalState state, String key,
                               String label, List<UiControlOptions.Choice> options, int width, int height,
                               int textColor, int fillColor, int borderColor, int accentColor,
                               int hoverColor, int disabledColor, int focusColor, JsonObject visualSettings) {
        super(0, 0, width, height, Component.literal(label));
        this.context = context;
        this.kind = kind;
        this.state = state;
        this.key = key;
        this.label = label;
        this.options = options;
        this.textColor = textColor;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.accentColor = accentColor;
        this.hoverColor = hoverColor;
        this.disabledColor = disabledColor;
        this.focusColor = focusColor;
        textures = new StateControlTextures(context, visualSettings);
        optionAppearances = kind == Kind.SELECT ? options.stream()
                .map(choice -> SelectOptionAppearance.resolve(context, choice.style())).toList() : List.of();
        indicatorSize = dimension(visualSettings, "indicatorSize", 14, 4, 128);
        trackWidth = dimension(visualSettings, "trackWidth", 29, 8, 256);
        trackHeight = dimension(visualSettings, "trackHeight", 12, 6, 128);
        thumbWidth = dimension(visualSettings, "thumbWidth", 9, 2, 128);
        thumbHeight = dimension(visualSettings, "thumbHeight", 8, 2, 128);
        arrowSize = dimension(visualSettings, "arrowSize", 12, 4, 128);
        popupRowHeight = dimension(visualSettings, "popupRowHeight", POPUP_ROW_HEIGHT, 12, 128);
    }

    private static int dimension(JsonObject settings, String name, int defaultValue, int min, int max) {
        int value = GsonHelper.getAsInt(settings, name, defaultValue);
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
        return value;
    }

    public static StateControlWidget create(WidgetContext context, LoreEntity.UiElement element,
                                            LoreStyle.Element style, Kind kind) {
        if (!(context.screen() instanceof UiStateHost host)) {
            throw new IllegalArgumentException(kind.name().toLowerCase() + " requires an AnchorUI screen with Local UI State");
        }
        JsonObject settings = element.settings;
        String key = GsonHelper.getAsString(settings, "stateKey", "");
        if (key.isBlank()) {
            throw new IllegalArgumentException(kind.name().toLowerCase() + " requires a 'stateKey'");
        }
        UiLocalState state = host.uiState();
        JsonElement initial = state.get(key);
        List<UiControlOptions.Choice> options = List.of();
        if (kind == Kind.CHECKBOX || kind == Kind.SWITCH) {
            if (!initial.isJsonPrimitive() || !initial.getAsJsonPrimitive().isBoolean()) {
                throw new IllegalArgumentException(kind.name().toLowerCase() + " needs a boolean UI state key: " + key);
            }
        } else {
            options = UiControlOptions.parse(settings, state, key);
        }
        int width = style.size.width > 0 ? style.size.width : kind == Kind.TABS ? 220 : 150;
        int height = style.size.height > 0 ? style.size.height : 22;
        JsonObject visualSettings = style.resolvedSettings;
        int fillColor = visualSettings.has("fillColor") ? WidgetColors.parse(GsonHelper.getAsString(visualSettings, "fillColor"))
                : (style.background.color >>> 24) != 0 ? style.background.color : BASE;
        int borderColor = visualSettings.has("borderColor") ? WidgetColors.parse(GsonHelper.getAsString(visualSettings, "borderColor")) : BORDER;
        int accentColor = visualSettings.has("accentColor") ? WidgetColors.parse(GsonHelper.getAsString(visualSettings, "accentColor")) : SELECTED;
        int hoverColor = WidgetColors.parse(GsonHelper.getAsString(visualSettings, "hoverColor", "#FF36506A"));
        int disabledColor = WidgetColors.parse(GsonHelper.getAsString(visualSettings, "disabledColor", "#FF353B43"));
        int focusColor = WidgetColors.parse(GsonHelper.getAsString(visualSettings, "focusColor", "#FFFFD16B"));
        return new StateControlWidget(context, kind, state, key, element.text, options, width, height, style.textColor, fillColor, borderColor, accentColor, hoverColor, disabledColor, focusColor, visualSettings);
    }

    @Override
    public void setChangeListener(Consumer<JsonElement> listener) {
        changeListener = Objects.requireNonNull(listener, "UI change listener must not be null");
    }

    private int selectedIndex() {
        return UiControlOptions.selectedIndex(options, state.get(key));
    }

    private void select(int index) {
        if (index < 0 || index >= options.size()) {
            return;
        }
        JsonElement next = options.get(index).value();
        if (next.equals(state.get(key))) {
            return;
        }
        state.set(key, next);
        changeListener.accept(next);
    }

    private void toggle() {
        boolean next = state.toggle(key);
        changeListener.accept(new JsonPrimitive(next));
    }

    private void clickEvent() {
        if (context.screen() instanceof ItemLoreScreen screen) {
            screen.dispatchWidgetClick(this);
        }
    }

    @Override
    public void onPress() {
        switch (kind) {
            case CHECKBOX, SWITCH -> toggle();
            case SELECT -> {
                if (popupOpen) {
                    select(highlight);
                    popupOpen = false;
                } else {
                    highlight = Math.max(0, selectedIndex());
                    scrollStart = Math.max(0, Math.min(highlight - shownOptionCount() + 1, options.size() - shownOptionCount()));
                    if (context.screen() instanceof ItemLoreScreen screen) {
                        screen.closeOtherWidgetPopups(this);
                    }
                    popupOpen = true;
                }
            }
            case TABS -> select((Math.max(0, selectedIndex()) + 1) % options.size());
        }
        clickEvent();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            popupOpen = false;
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (kind == Kind.TABS) {
            int index = (int) (((mouseX - getX()) / Math.max(1, getWidth())) * options.size());
            select(Math.max(0, Math.min(index, options.size() - 1)));
            clickEvent();
        } else {
            super.onClick(mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible || !active) {
            return false;
        }
        if (kind == Kind.TABS) {
            if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
                int offset = keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1;
                select(Math.floorMod(Math.max(0, selectedIndex()) + offset, options.size()));
                clickEvent();
                return true;
            }
        }
        if (kind == Kind.SELECT && popupOpen) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                popupOpen = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
                highlight = Math.max(0, Math.min(options.size() - 1, highlight + (keyCode == GLFW.GLFW_KEY_UP ? -1 : 1)));
                scrollStart = Math.max(0, Math.min(scrollStart, highlight));
                scrollStart = Math.max(scrollStart, highlight - shownOptionCount() + 1);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!active) {
            popupOpen = false;
        }
        int x = getX();
        int y = getY();
        int height = getHeight();
        int width = getWidth();
        boolean hovered = isMouseOver(mouseX, mouseY);
        boolean focused = isFocused();
        if (!textures.render(graphics, "background", x, y, width, height, ControlVisualStates.background(active, hovered, focused))) {
            drawBox(graphics, x, y, width, height, !active ? disabledColor : hovered || focused ? hoverColor : fillColor);
        }

        switch (kind) {
            case CHECKBOX -> {
                int side = Math.max(8, Math.min(indicatorSize, Math.max(8, height - 6)));
                int boxX = x + 4;
                int boxY = y + (height - side) / 2;
                boolean checked = state.get(key).getAsBoolean();
                if (!drawToggleTexture(graphics, "indicator", boxX, boxY, side, side, checked, hovered)) {
                    drawBox(graphics, boxX, boxY, side, side, checked ? accentColor : fillColor);
                    if (checked) {
                        graphics.fill(boxX + 3, boxY + 3, boxX + side - 3, boxY + side - 3, TEXT);
                    }
                }
                drawText(graphics, label, boxX + side + 6, y + (height - 8) / 2, Math.max(0, width - side - 16), textColor);
            }
            case SWITCH -> {
                int trackX = x + 5;
                int trackY = y + (height - trackHeight) / 2;
                boolean enabled = state.get(key).getAsBoolean();
                if (!drawToggleTexture(graphics, "track", trackX, trackY, trackWidth, trackHeight, enabled, hovered)) {
                    drawBox(graphics, trackX, trackY, trackWidth, trackHeight, enabled ? accentColor : HOVER);
                }
                int knobX = trackX + (enabled ? Math.max(2, trackWidth - thumbWidth - 2) : 2);
                int knobY = trackY + (trackHeight - thumbHeight) / 2;
                if (!drawToggleTexture(graphics, "thumb", knobX, knobY, thumbWidth, thumbHeight, enabled, hovered)) {
                    graphics.fill(knobX, knobY, knobX + thumbWidth, knobY + thumbHeight, TEXT);
                }
                drawText(graphics, label, trackX + trackWidth + 6, y + (height - 8) / 2, Math.max(0, width - trackWidth - 16), textColor);
            }
            case SELECT -> {
                int index = selectedIndex();
                String value = index < 0 ? "" : options.get(index).label();
                String description = label.isBlank() ? value : label + ": " + value;
                drawText(graphics, description, x + 7, y + (height - 8) / 2, Math.max(0, width - 24), textColor);
                int arrowX = x + width - arrowSize - 3;
                if (!textures.render(graphics, "arrow", arrowX, y + (height - arrowSize) / 2, arrowSize, arrowSize, !active ? "disabled" : popupOpen ? "open" : "closed", "closed")) {
                    drawText(graphics, popupOpen ? "▲" : "▼", x + width - 15, y + (height - 8) / 2, 12, MUTED);
                }
            }
            case TABS -> {
                int count = options.size();
                int chosen = selectedIndex();
                for (int index = 0; index < count; index++) {
                    int left = x + index * width / count;
                    int right = x + (index + 1) * width / count;
                    boolean tabHover = hovered && mouseX >= left && mouseX < right;
                    boolean selected = index == chosen;
                    boolean rendered = selected
                            ? textures.render(graphics, "tab", left, y, right - left, height, !active ? "disabled" : tabHover ? "selectedHover" : "selected", "selected")
                            : textures.render(graphics, "tab", left, y, right - left, height, !active ? "disabled" : tabHover ? "hover" : "normal");
                    if (!rendered) {
                        if (selected) {
                            graphics.fill(left + 1, y + 1, right - 1, y + height - 1, accentColor);
                        }
                        if (index > 0) {
                            graphics.fill(left, y + 2, left + 1, y + height - 2, borderColor);
                        }
                    }
                    String title = options.get(index).label();
                    int maxWidth = Math.max(0, right - left - 8);
                    String shortTitle = context.font().plainSubstrByWidth(title, maxWidth);
                    int tx = left + Math.max(2, (right - left - context.font().width(shortTitle)) / 2);
                    drawText(graphics, shortTitle, tx, y + (height - 8) / 2, maxWidth, index == chosen ? TEXT : MUTED);
                }
            }
        }
        if (ControlVisualStates.needsFocusOutline(textures, active, hovered, focused)) {
            ControlVisualStates.renderFocusOutline(graphics, x, y, width, height, focusColor);
        }
    }

    private boolean drawToggleTexture(GuiGraphics graphics, String part, int x, int y, int width, int height,
                                      boolean on, boolean hovered) {
        String base = on ? "on" : "off";
        if (!active) {
            return textures.render(graphics, part, x, y, width, height, on ? "onDisabled" : "offDisabled", base);
        }
        if (hovered) {
            return textures.render(graphics, part, x, y, width, height, on ? "onHover" : "offHover", base);
        }
        return textures.render(graphics, part, x, y, width, height, base);
    }

    private void drawBox(GuiGraphics graphics, int x, int y, int width, int height, int fill) {
        if (width < 2 || height < 2) {
            return;
        }
        graphics.fill(x, y, x + width, y + height, borderColor);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
    }

    private void drawText(GuiGraphics graphics, String text, int x, int y, int availableWidth, int color) {
        if (availableWidth > 0 && !text.isEmpty()) {
            graphics.drawString(context.font(), context.font().plainSubstrByWidth(text, availableWidth), x, y, color, false);
        }
    }

    private int shownOptionCount() {
        int below = Math.max(0, context.screen().height - getBottom() - 2);
        int above = Math.max(0, getY() - 2);
        int availableRows = Math.max(1, (Math.max(below, above) - 2) / popupRowHeight);
        return Math.min(options.size(), Math.min(MAX_VISIBLE_OPTIONS, availableRows));
    }

    private int popupY() {
        int popupHeight = shownOptionCount() * popupRowHeight + 2;
        int below = Math.max(0, context.screen().height - getBottom() - 2);
        int above = Math.max(0, getY() - 2);
        return below < popupHeight && above > below ? getY() - popupHeight : getBottom();
    }

    @Override
    public boolean containsPopup(double mouseX, double mouseY) {
        if (!isPopupOpen()) {
            return false;
        }
        int top = popupY();
        return mouseX >= getX() && mouseX < getRight()
                && mouseY >= top && mouseY < top + shownOptionCount() * popupRowHeight + 2;
    }

    @Override
    public boolean isPopupOpen() {
        return kind == Kind.SELECT && popupOpen && active && visible;
    }

    @Override
    public boolean mouseClickedPopup(double mouseX, double mouseY, int button) {
        if (!isPopupOpen()) {
            return false;
        }
        if (!containsPopup(mouseX, mouseY)) {
            return false;
        }
        int top = popupY();
        if (button == 0) {
            int index = scrollStart + (int) Math.max(0, (mouseY - top - 1) / popupRowHeight);
            select(Math.min(index, options.size() - 1));
            clickEvent();
        }
        popupOpen = false;
        return true;
    }

    @Override
    public boolean mouseScrolledPopup(double mouseX, double mouseY, double scrollY) {
        if (!containsPopup(mouseX, mouseY)) {
            return false;
        }
        scrollStart = Math.max(0, Math.min(options.size() - shownOptionCount(), scrollStart - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public void renderPopup(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isPopupOpen()) {
            return;
        }
        int x = getX();
        int y = popupY();
        int width = getWidth();
        int count = shownOptionCount();
        if (!textures.render(graphics, "popup", x, y, width, count * popupRowHeight + 2, "normal")) {
            drawBox(graphics, x, y, width, count * popupRowHeight + 2, fillColor);
        }
        int selected = selectedIndex();
        for (int row = 0; row < count; row++) {
            int index = scrollStart + row;
            int top = y + 1 + row * popupRowHeight;
            boolean hovered = mouseX >= x && mouseX < x + width
                    && mouseY >= top && mouseY < top + popupRowHeight;
            boolean selectedOption = index == selected;
            boolean optionHover = hovered || index == highlight;
            SelectOptionAppearance appearance = optionAppearances.get(index);
            if (!renderOptionBackground(graphics, appearance, x + 1, top, width - 2, popupRowHeight, selectedOption, optionHover) && (selectedOption || optionHover)) {
                graphics.fill(x + 1, top, x + width - 1, top + popupRowHeight, selectedOption ? accentColor : HOVER);
            }
            int defaultTextColor = selectedOption ? TEXT : textColor;
            drawText(graphics, options.get(index).label(), x + 7, top + (popupRowHeight - 8) / 2, width - 14, appearance == null ? defaultTextColor : appearance.textColor(selectedOption, optionHover, defaultTextColor));
        }
    }

    private boolean renderOptionBackground(GuiGraphics graphics, SelectOptionAppearance appearance,
                                           int x, int y, int width, int height,
                                           boolean selected, boolean hovered) {
        String[] statePriority = selected
                ? hovered ? new String[]{"selectedHover", "selected", "hover"} : new String[]{"selected"}
                : hovered ? new String[]{"hover"} : new String[0];
        if (appearance != null) {
            for (String state : statePriority) {
                if (appearance.hasBackgroundVariant(state)
                        && appearance.renderBackgroundVariant(graphics, x, y, width, height, state)) {
                    return true;
                }
            }
        }
        for (String state : statePriority) {
            if (textures.renderExact(graphics, "option", x, y, width, height, state)) {
                return true;
            }
        }
        return appearance != null && appearance.renderBackgroundVariant(graphics, x, y, width, height, "normal")
                || textures.renderExact(graphics, "option", x, y, width, height, "normal");
    }

    @Override
    public void closePopup() {
        popupOpen = false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        String current = switch (kind) {
            case CHECKBOX, SWITCH -> state.get(key).getAsBoolean() ? "enabled" : "disabled";
            case SELECT, TABS -> options.get(Math.max(0, selectedIndex())).label();
        };
        output.add(NarratedElementType.TITLE, Component.literal(label + ": " + current));
    }
}
