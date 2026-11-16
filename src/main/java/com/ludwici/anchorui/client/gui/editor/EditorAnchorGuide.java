package com.ludwici.anchorui.client.gui.editor;

import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.style.ElementAnchor;
import com.ludwici.anchorui.api.style.PositionElement;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Draws the selected F8 anchor connection in GUI-pixel coordinates. */
public final class EditorAnchorGuide {
    public record Point(int x, int y) {
    }

    public record Connection(Point widget, Point target, Point offsetTarget, int offsetX, int offsetY) {
    }

    private static final int WIDGET_COLOR = 0xFFFFD16B;
    private static final int TARGET_COLOR = 0xFF68E0DE;
    private static final int OFFSET_COLOR = 0xFFADD7E8;

    private EditorAnchorGuide() {
    }

    public static Point point(AnchorLayout.Bounds bounds, ElementAnchor anchor) {
        return new Point(bounds.x() + anchor.x(bounds.width()), bounds.y() + anchor.y(bounds.height()));
    }

    public static Connection connect(AnchorLayout.Bounds widget, AnchorLayout.Bounds target, PositionElement.Constraint anchor) {
        Point from = point(widget, anchor.point());
        Point to = point(target, anchor.anchor());
        return new Connection(from, to, new Point(bounded((long) to.x() + anchor.x()), bounded((long) to.y() + anchor.y())), anchor.x(), anchor.y());
    }

    public static void draw(GuiGraphics graphics, Font font, Connection guide, int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 180.0F);
        try {
            dottedLine(graphics, guide.target(), guide.widget(), screenWidth, screenHeight, TARGET_COLOR);
            if (!guide.offsetTarget().equals(guide.widget())) {
                dottedLine(graphics, guide.offsetTarget(), guide.widget(), screenWidth, screenHeight, WIDGET_COLOR);
            }
            mark(graphics, guide.target(), TARGET_COLOR, screenWidth, screenHeight);
            if (!guide.offsetTarget().equals(guide.target()) && !guide.offsetTarget().equals(guide.widget())) {
                mark(graphics, guide.offsetTarget(), OFFSET_COLOR, screenWidth, screenHeight);
            }
            mark(graphics, guide.widget(), WIDGET_COLOR, screenWidth, screenHeight);
            String offset = "X " + signed(guide.offsetX()) + "  Y " + signed(guide.offsetY());
            int labelWidth = font.width(offset) + 6;
            int x = bounded(Math.max(1L, Math.min(((long) guide.target().x() + guide.widget().x()) / 2 + 6, (long) screenWidth - labelWidth - 1)));
            int y = bounded(Math.max(1L, Math.min(((long) guide.target().y() + guide.widget().y()) / 2 - 13, (long) screenHeight - 12)));
            graphics.fill(x - 1, y - 1, x + labelWidth + 1, y + 11, 0xE01A2836);
            graphics.drawString(font, offset, x + 2, y + 1, 0xFFE4F6F7, false);
        } finally {
            graphics.pose().popPose();
        }
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private static int bounded(long value) {
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    private static void mark(GuiGraphics graphics, Point point, int color, int screenWidth, int screenHeight) {
        int x = point.x();
        int y = point.y();
        if (x >= 3 && y >= 3 && x < screenWidth - 3 && y < screenHeight - 3) {
            graphics.fill(x - 3, y - 3, x + 4, y + 4, 0xEA10202A);
            graphics.fill(x - 2, y, x + 3, y + 1, color);
            graphics.fill(x, y - 2, x + 1, y + 3, color);
        }
    }

    private static void dottedLine(GuiGraphics graphics, Point from, Point to, int screenWidth, int screenHeight, int color) {
        long dx = (long) to.x() - from.x();
        long dy = (long) to.y() - from.y();
        long distance = Math.max(Math.abs(dx), Math.abs(dy));
        if (distance == 0) {
            return;
        }
        int samples = (int) Math.max(1L, Math.min(512L, distance / 3L));
        for (int step = 0; step <= samples; step++) {
            long x = from.x() + dx * step / samples;
            long y = from.y() + dy * step / samples;
            if (x >= 0 && y >= 0 && x < screenWidth && y < screenHeight) {
                graphics.fill((int) x, (int) y, (int) x + 1, (int) y + 1, color);
            }
        }
    }
}
