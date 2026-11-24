package com.ludwici.anchorui.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Vector3f;

final class TextClip {
    private TextClip() {
    }

    static void enable(GuiGraphics graphics, int left, int top, int right, int bottom) {
        var pose = graphics.pose().last().pose();
        Vector3f a = pose.transformPosition(new Vector3f(left, top, 0));
        Vector3f b = pose.transformPosition(new Vector3f(right, top, 0));
        Vector3f c = pose.transformPosition(new Vector3f(left, bottom, 0));
        Vector3f d = pose.transformPosition(new Vector3f(right, bottom, 0));
        int x1 = (int) Math.floor(Math.min(Math.min(a.x, b.x), Math.min(c.x, d.x)));
        int y1 = (int) Math.floor(Math.min(Math.min(a.y, b.y), Math.min(c.y, d.y)));
        int x2 = (int) Math.ceil(Math.max(Math.max(a.x, b.x), Math.max(c.x, d.x)));
        int y2 = (int) Math.ceil(Math.max(Math.max(a.y, b.y), Math.max(c.y, d.y)));
        graphics.enableScissor(x1, y1, x2, y2);
    }
}
