package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.animation.UiAnimationSpec;
import com.ludwici.anchorui.api.animation.UiAnimationTimeline;
import com.ludwici.anchorui.api.animation.UiAnimationTriggerRegistry;
import net.minecraft.resources.ResourceLocation;
import com.ludwici.anchorui.api.binding.BoundData;
import com.ludwici.anchorui.api.binding.DataBindingSpec;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.layout.WidgetTransforms;
import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public final class WidgetAnimation {
    private final UiAnimationTimeline timeline;
    private final Map<String, BoundData> watches;
    private final Set<ResourceLocation> customTriggerIds;
    private final Set<ResourceLocation> warnedCustomTriggers = new HashSet<>();
    private final WidgetContext context;

    private WidgetAnimation(UiAnimationTimeline timeline, Map<String, BoundData> watches, Set<ResourceLocation> customTriggerIds, WidgetContext context) {
        this.timeline = timeline;
        this.watches = Map.copyOf(watches);
        this.customTriggerIds = Set.copyOf(customTriggerIds);
        this.context = context;
    }

    public static WidgetAnimation parse(JsonObject settings, JsonArray defaults, WidgetContext context) {
        UiAnimationSpec spec = UiAnimationSpec.parse(settings, defaults);
        if (spec.isEmpty()) {
            return null;
        }
        Map<String, BoundData> watches = new HashMap<>();
        Set<ResourceLocation> customIds = new HashSet<>();
        for (UiAnimationSpec.Track track : spec.tracks()) {
            if (!track.enabled()) {
                continue;
            }
            if (track.customTrigger() != null) {
                customIds.add(track.customTrigger());
            }
            if (track.watch() == null) {
                continue;
            }
            JsonObject holder = new JsonObject();
            holder.add("watch", track.watch());
            BoundData bound = BoundData.parse(holder, "watch", new JsonPrimitive(""), DataBindingSpec.Kind.ANY);
            if (!bound.isBound()) {
                throw new IllegalArgumentException("Animation '" + track.id() + "' watch must have a bound data source");
            }
            watches.put(track.id(), bound);
        }
        return new WidgetAnimation(new UiAnimationTimeline(spec), watches, customIds, context);
    }

    public void update(boolean visible, long now) {
        Map<String, JsonElement> values = new HashMap<>();
        for (var entry : watches.entrySet()) {
            values.put(entry.getKey(), entry.getValue().get(context));
        }
        Map<ResourceLocation, Long> revisions = new HashMap<>();
        if (visible) {
            for (ResourceLocation id : customTriggerIds) {
                if (!UiAnimationTriggerRegistry.contains(id)) {
                    if (warnedCustomTriggers.add(id)) {
                        AnchorUI.LOGGER.warn("Unknown AnchorUI animation trigger '{}'; register it in Java before opening the UI", id);
                    }
                    continue;
                }
                try {
                    UiAnimationTriggerRegistry.sample(id, context).ifPresent(revision -> revisions.put(id, revision));
                    warnedCustomTriggers.remove(id);
                } catch (RuntimeException exception) {
                    if (warnedCustomTriggers.add(id)) {
                        AnchorUI.LOGGER.error("AnchorUI animation trigger '{}' failed during evaluation; retaining the previous animation state", id, exception);
                    }
                }
            }
        }
        timeline.update(visible, values, revisions, now);
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

    public static AnchorLayout.Bounds physicalBounds(AnchorLayout.Bounds logical,
                                                      WidgetTransforms.Transform parent, UiAnimationSpec.Frame frame) {
        double cx = logical.x() + logical.width() / 2.0;
        double cy = logical.y() + logical.height() / 2.0;
        double left = parent.x(frame.x() + cx + (logical.x() - cx) * frame.scale());
        double top = parent.y(frame.y() + cy + (logical.y() - cy) * frame.scale());
        double right = parent.x(frame.x() + cx + (logical.x() + logical.width() - cx) * frame.scale());
        double bottom = parent.y(frame.y() + cy + (logical.y() + logical.height() - cy) * frame.scale());
        int x = (int) Math.floor(left);
        int y = (int) Math.floor(top);
        return new AnchorLayout.Bounds(x, y, Math.max(0, (int) Math.ceil(right) - x), Math.max(0, (int) Math.ceil(bottom) - y));
    }

    public static double inverseX(AbstractWidget widget, UiAnimationSpec.Frame frame, double coordinate) {
        return frame.inverseX(coordinate, widget.getX() + widget.getWidth() / 2.0);
    }

    public static double inverseY(AbstractWidget widget, UiAnimationSpec.Frame frame, double coordinate) {
        return frame.inverseY(coordinate, widget.getY() + widget.getHeight() / 2.0);
    }
}
