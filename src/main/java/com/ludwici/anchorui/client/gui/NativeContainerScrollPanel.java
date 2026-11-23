package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.layout.AnchorLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Loader-neutral scrollbar for scrollable widget containers and editor inspectors. */
public final class NativeContainerScrollPanel {
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
    private final int logicalLimit;
    private final int wheelStep;
    private final double scale;
    private final int maxScroll;
    private float scrollDistance;
    private boolean dragging;
    private double dragStartY;
    private float dragStartScroll;

    public NativeContainerScrollPanel(Minecraft minecraft, AnchorLayout.Bounds viewport, int logicalLimit, int wheelStep, double scale, int initialOffset) {
        this(minecraft, viewport, logicalLimit, wheelStep, scale, initialOffset, CONTENT);
    }

    public NativeContainerScrollPanel(Minecraft minecraft, AnchorLayout.Bounds viewport, int logicalLimit, int wheelStep, double scale, int initialOffset, Style style) {
        if (viewport.width() <= style.width() || viewport.height() < 34 || logicalLimit <= 0 || !Double.isFinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("Invalid scrollbar geometry or range");
        }
        this.style = style;
        this.viewport = viewport;
        this.logicalLimit = logicalLimit;
        this.wheelStep = Math.max(1, (int) Math.round(wheelStep * scale));
        this.scale = scale;
        this.maxScroll = Math.max(1, (int) Math.round(logicalLimit * scale));
        setLogicalOffset(initialOffset);
    }

    public boolean matches(AnchorLayout.Bounds viewport, int logicalLimit, int wheelStep, double scale) {
        return matches(viewport, logicalLimit, wheelStep, scale, CONTENT);
    }

    public boolean matches(AnchorLayout.Bounds viewport, int logicalLimit, int wheelStep, double scale, Style style) {
        return this.viewport.equals(viewport) && this.logicalLimit == logicalLimit && this.wheelStep == Math.max(1, (int) Math.round(wheelStep * scale)) && Double.compare(this.scale, scale) == 0 && this.style.equals(style);
    }

    public int logicalOffset() {
        return Math.max(0, Math.min(logicalLimit, (int) Math.round(scrollDistance / scale)));
    }

    public void setLogicalOffset(int offset) {
        scrollDistance = clamp((float) (Math.max(0, Math.min(logicalLimit, offset)) * scale));
    }

    public boolean isDraggingBar() {
        return dragging;
    }

    public boolean isMouseOver(double x, double y) {
        return x >= viewport.x() && x < viewport.x() + viewport.width() && y >= viewport.y() && y < viewport.y() + viewport.height();
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int trackX = viewport.x() + viewport.width() - style.width();
        int bottom = viewport.y() + viewport.height();
        graphics.fill(trackX, viewport.y(), trackX + style.width(), bottom, style.trackColor());
        int start = thumbStart();
        graphics.fill(trackX, start, trackX + style.width(), start + thumbHeight(), style.thumbColor());
        graphics.fill(trackX, start, trackX + 1, start + thumbHeight(), style.borderColor());
    }

    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0 || !isOverTrack(x, y)) {
            return false;
        }
        int start = thumbStart();
        if (y < start || y >= start + thumbHeight()) {
            float fraction = (float) ((y - viewport.y() - thumbHeight() / 2.0) / Math.max(1, viewport.height() - thumbHeight()));
            scrollDistance = clamp(fraction * maxScroll);
        }
        dragging = true;
        dragStartY = y;
        dragStartScroll = scrollDistance;
        return true;
    }

    public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        if (!dragging || button != 0) {
            return false;
        }
        float delta = (float) ((y - dragStartY) * maxScroll / Math.max(1, viewport.height() - thumbHeight()));
        scrollDistance = clamp(dragStartScroll + delta);
        return true;
    }

    public boolean mouseReleased(double x, double y, int button) {
        if (!dragging || button != 0) {
            return false;
        }
        dragging = false;
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scrollDistance = clamp(scrollDistance - (float) scrollY * wheelStep);
        return true;
    }

    private boolean isOverTrack(double x, double y) {
        int trackX = viewport.x() + viewport.width() - style.width();
        int hitWidth = style == HUD_INSPECTOR || style == SCREEN_INSPECTOR ? 6 : style.width();
        return x >= trackX - (hitWidth - style.width()) && x < trackX + style.width()
                && y >= viewport.y() && y < viewport.y() + viewport.height();
    }

    private int thumbHeight() {
        return Math.min(viewport.height(), Math.max(12, (int) Math.round((double) viewport.height() * viewport.height() / (viewport.height() + maxScroll))));
    }

    private int thumbStart() {
        return viewport.y() + Math.round((viewport.height() - thumbHeight()) * scrollDistance / maxScroll);
    }

    private float clamp(float distance) {
        return Math.max(0, Math.min(maxScroll, distance));
    }
}
