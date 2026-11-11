package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.neoforged.neoforge.client.gui.widget.ScrollPanel;

public final class NativeContainerScrollPanel extends ScrollPanel {
    private static final int BAR_WIDTH = 6;

    private final AnchorLayout.Bounds viewport;
    private final int contentHeight;
    private final int logicalLimit;
    private final int wheelStep;
    private final double scale;
    private boolean dragging;

    public NativeContainerScrollPanel(Minecraft minecraft, AnchorLayout.Bounds viewport, int logicalLimit,
                                      int wheelStep, double scale, int initialOffset) {
        super(minecraft, viewport.width(), viewport.height(), viewport.y(), viewport.x(), 0, BAR_WIDTH,
                0x88415A70, 0xFFD5DFED, 0xFF9BACBE);
        if (viewport.width() <= BAR_WIDTH || viewport.height() < 8 || logicalLimit <= 0 || !Double.isFinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("Invalid native scrollbar geometry or range");
        }
        this.viewport = viewport;
        this.logicalLimit = logicalLimit;
        this.wheelStep = Math.max(1, (int) Math.round(wheelStep * scale));
        this.scale = scale;
        this.contentHeight = viewport.height() + Math.max(1, (int) Math.round(logicalLimit * scale));
        this.scrollDistance = Math.max(0, Math.min(contentHeight - viewport.height(),
                (float) (initialOffset * scale)));
    }

    public boolean matches(AnchorLayout.Bounds viewport, int logicalLimit, int wheelStep, double scale) {
        return this.viewport.equals(viewport) && this.logicalLimit == logicalLimit
                && this.wheelStep == Math.max(1, (int) Math.round(wheelStep * scale))
                && Double.compare(this.scale, scale) == 0;
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
