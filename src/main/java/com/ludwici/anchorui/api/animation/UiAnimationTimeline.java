package com.ludwici.anchorui.api.animation;

import com.google.gson.JsonElement;
import com.ludwici.anchorui.api.animation.UiAnimationSpec.Frame;
import com.ludwici.anchorui.api.animation.UiAnimationSpec.Track;
import com.ludwici.anchorui.api.animation.UiAnimationSpec.Trigger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UiAnimationTimeline {
    private record Running(Track track, long startNanos, Frame from) {
        private long delayNanos() {
            return track.delayMillis() * 1000000L;
        }

        private boolean pending(long now) {
            return now < startNanos + delayNanos();
        }

        private boolean finished(long now) {
            return now - startNanos >= track.totalMillis() * 1000000L;
        }

        private Frame frame(long now) {
            if (pending(now)) {
                return track.trigger() == Trigger.OPEN || track.trigger() == Trigger.SHOW ? from : Frame.IDENTITY;
            }
            double elapsed = Math.max(0.0, (now - startNanos - delayNanos()) / (track.durationMillis() * 1000000.0));
            return from.between(track.to(), track.easing().apply(elapsed));
        }
    }

    private final UiAnimationSpec spec;
    private final Map<String, Running> running = new LinkedHashMap<>();
    private final Map<String, JsonElement> watched = new HashMap<>();
    private boolean initialized;
    private boolean visible;

    public UiAnimationTimeline(UiAnimationSpec spec) {
        this.spec = spec;
    }

    public void update(boolean visible, Map<String, JsonElement> values, long nowNanos) {
        if (!initialized) {
            initialized = true;
            this.visible = visible;
            remember(values);
            if (visible) {
                begin(Trigger.OPEN, nowNanos);
            }
            return;
        }
        if (this.visible != visible) {
            this.visible = visible;
            if (visible) {
                removeTriggers(Trigger.HIDE, Trigger.CLOSE);
                begin(Trigger.SHOW, nowNanos);
            } else {
                removeTriggers(Trigger.OPEN, Trigger.SHOW, Trigger.CHANGE);
                begin(Trigger.HIDE, nowNanos);
            }
        } else if (visible) {
            for (Track track : spec.tracks()) {
                if (track.enabled() && track.trigger() == Trigger.CHANGE) {
                    JsonElement next = values == null ? null : values.get(track.id());
                    JsonElement previous = watched.get(track.id());
                    if (next != null && previous != null && !next.equals(previous)) {
                        launch(track, nowNanos);
                    }
                }
            }
        }
        remember(values);
    }

    private void remember(Map<String, JsonElement> values) {
        if (values == null) {
            return;
        }
        for (var entry : values.entrySet()) {
            if (entry.getValue() == null) {
                watched.remove(entry.getKey());
            } else {
                watched.put(entry.getKey(), entry.getValue().deepCopy());
            }
        }
    }

    private void removeTriggers(Trigger... triggers) {
        for (var iterator = running.entrySet().iterator(); iterator.hasNext();) {
            Running item = iterator.next().getValue();
            for (Trigger trigger : triggers) {
                if (item.track().trigger() == trigger) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    public void begin(Trigger trigger, long nowNanos) {
        if (trigger == Trigger.CLOSE) {
            running.clear();
        }
        for (Track track : spec.tracks()) {
            if (track.enabled() && track.trigger() == trigger) {
                launch(track, nowNanos);
            }
        }
    }

    private void launch(Track track, long nowNanos) {
        Running current = running.get(track.id());
        Frame initial = current != null && !current.finished(nowNanos) && !current.pending(nowNanos)
                ? current.frame(nowNanos) : track.from();
        running.put(track.id(), new Running(track, nowNanos, initial));
    }

    public Frame sample(long nowNanos) {
        double opacity = 1.0;
        double x = 0.0;
        double y = 0.0;
        double scale = 1.0;
        for (Running item : running.values()) {
            Frame frame = item.frame(nowNanos);
            opacity *= frame.opacity();
            x += frame.x();
            y += frame.y();
            scale *= frame.scale();
        }
        return new Frame(Math.max(0.0, Math.min(1.0, opacity)), Math.max(-4096.0, Math.min(4096.0, x)), Math.max(-4096.0, Math.min(4096.0, y)), Math.max(0.01, Math.min(4.0, scale)));
    }

    public boolean finished(long nowNanos) {
        return running.values().stream().allMatch(item -> item.finished(nowNanos));
    }

    public boolean renderWhenHidden(long nowNanos) {
        return running.values().stream().anyMatch(item -> item.track().trigger() == Trigger.HIDE && !item.finished(nowNanos));
    }

    public long closeDurationMillis() {
        return spec.tracks().stream().filter(item -> item.enabled() && item.trigger() == Trigger.CLOSE)
                .mapToLong(Track::totalMillis).max().orElse(0L);
    }
}
