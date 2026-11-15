package com.ludwici.anchorui.api.animation;

import com.google.gson.JsonElement;
import com.ludwici.anchorui.api.animation.UiAnimationSpec.Frame;
import com.ludwici.anchorui.api.animation.UiAnimationSpec.Track;
import com.ludwici.anchorui.api.animation.UiAnimationSpec.Trigger;

public final class UiAnimationTimeline {
    private final UiAnimationSpec spec;
    private boolean initialized;
    private boolean lastVisible;
    private JsonElement lastWatched;
    private Track active;
    private long startedAtNanos;
    private Frame start = Frame.IDENTITY;
    private Frame end = Frame.IDENTITY;
    private boolean finishingHide;

    public UiAnimationTimeline(UiAnimationSpec spec) {
        this.spec = spec;
    }

    public void update(boolean visible, JsonElement watched, long nowNanos) {
        if (!initialized) {
            initialized = true;
            lastVisible = visible;
            lastWatched = watched == null ? null : watched.deepCopy();
            if (visible) {
                begin(Trigger.OPEN, nowNanos);
            }
            return;
        }
        if (visible != lastVisible) {
            lastVisible = visible;
            if (visible) {
                finishingHide = false;
                begin(Trigger.SHOW, nowNanos);
            } else {
                finishingHide = false;
                begin(Trigger.HIDE, nowNanos);
                finishingHide = active != null;
            }
        } else if (watched != null && lastWatched != null && !watched.equals(lastWatched)) {
            begin(Trigger.CHANGE, nowNanos);
        }
        lastWatched = watched == null ? null : watched.deepCopy();
    }

    public void begin(Trigger trigger, long nowNanos) {
        Track track = spec.track(trigger);
        if (track == null) {
            active = null;
            return;
        }
        // Sample the old animation first so successive changes do not snap back to its start.
        Frame initial = active == null || finished(nowNanos) ? track.from() : sample(nowNanos);
        active = track;
        start = initial;
        end = track.to();
        startedAtNanos = nowNanos;
        if (trigger != Trigger.HIDE) {
            finishingHide = false;
        }
    }

    public Frame sample(long nowNanos) {
        if (active == null) {
            return Frame.IDENTITY;
        }
        double elapsed = Math.max(0.0, (nowNanos - startedAtNanos) / (active.durationMillis() * 1000000.0));
        return start.between(end, active.easing().apply(elapsed));
    }

    public boolean finished(long nowNanos) {
        return active == null || nowNanos - startedAtNanos >= active.durationMillis() * 1000000L;
    }

    public boolean renderWhenHidden(long nowNanos) {
        return finishingHide && !finished(nowNanos);
    }

    public long closeDurationMillis() {
        Track track = spec.track(Trigger.CLOSE);
        return track == null ? 0L : track.durationMillis();
    }
}
