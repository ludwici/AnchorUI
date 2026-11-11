package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;

public final class NativeContainerScrollPanel extends ScrollPanel {
    public record Style(int width, int trackColor, int thumbColor, int borderColor) {
        public Style {
            if (width < 1 || width > 16) {
                throw new IllegalArgumentException("Scrollbar width must be between 1 and 16");
            }
        }
    }

    public static final Style CONTENT = new Style(6, 0x88415A70, 0xFFD5DFED, 0xFF9BACBE);
    public static final Style WIDGET_LIST = new Style(2, 0xFF374756, 0xFF9FBDD0, 0xFF9FBDD0);
    public static final Style HUD_INSPECTOR = new Style(2, 0x66465C6F, 0xFFD5DDEB, 0xFFD5DDEB);
    public static final Style SCREEN_INSPECTOR = new Style(2, 0xFF465C6F, 0xFFD5DDEB, 0xFFD5DDEB);

    private final Style style;
    private final AnchorLayout.Bounds viewport;
    private final int contentHeight;
    private final int logicalLimit;
    private final int wheelStep;
    private final double scale;
    private boolean dragging;

    public NativeContainerScrollPanel(Minecraft minecraft, AnchorLayout.Bounds viewport, int logicalLimit,
                                      int wheelStep, double scale, int initialOffset) {
        this(minecraft, viewport, logicalLimit, wheelStep, scale, initialOffset, CONTENT);
    }

    public NativeContainerScrollPanel(Minecraft minecraft, AnchorLayout.Bounds viewport, int logicalLimit,
                                      int wheelStep, double scale, int initialOffset, Style style) {
        super(minecraft, viewport.width(), viewport.height(), viewport.y(), viewport.x(), 0, style.width(),
                style.trackColor(), style.thumbColor(), style.borderColor());
        if (viewport.width() <= style.width() || viewport.height() < 34 || logicalLimit <= 0
                || !Double.isFinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("Invalid native scrollbar geometry or range");
        }
        this.style = style;
        this.viewport = viewport;
        this.logicalLimit = logicalLimit;
        this.wheelStep = Math.max(1, (int) Math.round(wheelStep * scale));
        this.scale = scale;
        this.contentHeight = viewport.height() + Math.max(1, (int) Math.round(logicalLimit * scale));
        this.scrollDistance = Math.max(0, Math.min(contentHeight - viewport.height(),
                (float) (initialOffset * scale)));
    }

    public boolean matches(AnchorLayout.Bounds viewport, int logicalLimit, int wheelStep, double scale) {
        return matches(viewport, logicalLimit, wheelStep, scale, CONTENT);
    }

    public boolean matches(AnchorLayout.Bounds viewport, int logicalLimit, int wheelStep, double scale, Style style) {
        return this.viewport.equals(viewport) && this.logicalLimit == logicalLimit
                && this.wheelStep == Math.max(1, (int) Math.round(wheelStep * scale))
                && Double.compare(this.scale, scale) == 0 && this.style.equals(style);
    }

    public int logicalOffset() {
        return Math.max(0, Math.min(logicalLimit, (int) Math.round(scrollDistance / scale)));
    }

    public void setLogicalOffset(int offset) {
        scrollDistance = Math.max(0, Math.min(contentHeight - viewport.height(),
                (float) (Math.max(0, Math.min(logicalLimit, offset)) * scale)));
    }

    public boolean isDraggingBar() {
        return dragging;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
    }

    @Override
    protected int getContentHeight() {
        return contentHeight;
    }

    @Override
    protected int getScrollAmount() {
        return wheelStep;
    }

    @Override
    protected void drawBackground(GuiGraphics graphics, Tesselator tessellator, float partialTick) {
    }

    @Override
    protected void drawPanel(GuiGraphics graphics, int right, int relativeY,
                             Tesselator tessellator, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled && button == 0) {
            dragging = true;
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return dragging && super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging) {
            return false;
        }
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return isMouseOver(mouseX, mouseY) && super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
