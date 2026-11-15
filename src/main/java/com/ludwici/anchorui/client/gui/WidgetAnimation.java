package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.api.animation.UiAnimationSpec;
import com.ludwici.anchorui.api.animation.UiAnimationTimeline;
import com.ludwici.anchorui.api.binding.BoundData;
import com.ludwici.anchorui.api.binding.DataBindingSpec;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

public final class WidgetAnimation {
    private final UiAnimationTimeline timeline;
    private final BoundData watch;
    private final WidgetContext context;

    private WidgetAnimation(UiAnimationTimeline timeline, BoundData watch, WidgetContext context) {
        this.timeline = timeline;
        this.watch = watch;
        this.context = context;
    }

    public static WidgetAnimation parse(JsonObject settings, JsonObject defaults, WidgetContext context) {
        UiAnimationSpec spec = UiAnimationSpec.parse(settings, defaults);
        if (spec.isEmpty()) {
            return null;
        }
        BoundData bound = null;
        JsonElement watched = spec.watch();
        if (watched != null) {
            JsonObject root = new JsonObject();
            root.add("watch", watched);
            bound = BoundData.parse(root, "watch", new JsonPrimitive(""), DataBindingSpec.Kind.ANY);
            if (!bound.isBound()) {
                throw new IllegalArgumentException("animation.watch must be a data binding with 'bind'");
            }
        }
        return new WidgetAnimation(new UiAnimationTimeline(spec), bound, context);
    }

    public void update(boolean visible, long now) {
        timeline.update(visible, watch == null ? null : watch.get(context), now);
    }

    public UiAnimationSpec.Frame frame(long now) {
        return timeline.sample(now);
    }

    public boolean renderWhenHidden(long now) {
        return timeline.renderWhenHidden(now);
    }

    public long closeDurationMillis() {
        return timeline.closeDurationMillis();
    }

    public void beginClose(long now) {
        timeline.begin(UiAnimationSpec.Trigger.CLOSE, now);
    }

    public static void applyPose(GuiGraphics graphics, AbstractWidget widget, UiAnimationSpec.Frame frame) {
        graphics.pose().translate(frame.x(), frame.y(), 0.0);
        if (frame.scale() != 1.0) {
            double centerX = widget.getX() + widget.getWidth() / 2.0;
            double centerY = widget.getY() + widget.getHeight() / 2.0;
            graphics.pose().translate(centerX, centerY, 0.0);
            graphics.pose().scale((float) frame.scale(), (float) frame.scale(), 1.0F);
            graphics.pose().translate(-centerX, -centerY, 0.0);
        }
    }

    public static double inverseX(AbstractWidget widget, UiAnimationSpec.Frame frame, double coordinate) {
        return frame.inverseX(coordinate, widget.getX() + widget.getWidth() / 2.0);
    }

    public static double inverseY(AbstractWidget widget, UiAnimationSpec.Frame frame, double coordinate) {
        return frame.inverseY(coordinate, widget.getY() + widget.getHeight() / 2.0);
    }
}
