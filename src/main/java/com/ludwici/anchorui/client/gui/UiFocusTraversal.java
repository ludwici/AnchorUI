package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.layout.AnchorLayout;

import java.util.ArrayList;
import java.util.List;

public final class UiFocusTraversal {
    public record Candidate(String id, boolean visible, boolean active,
                            AnchorLayout.Bounds bounds, AnchorLayout.Bounds clip) {
    }

    private UiFocusTraversal() {
    }

    public static boolean reachable(Candidate candidate, AnchorLayout.Bounds viewport) {
        return candidate.visible() && candidate.active()
                && intersects(candidate.bounds(), viewport)
                && (candidate.clip() == null || intersects(candidate.bounds(), candidate.clip()));
    }

    public static String next(List<Candidate> candidates, AnchorLayout.Bounds viewport,
                              String currentId, boolean backwards) {
        List<String> available = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (reachable(candidate, viewport)) {
                available.add(candidate.id());
            }
        }
        if (available.isEmpty()) {
            return null;
        }
        int current = available.indexOf(currentId);
        if (current < 0) {
            return backwards ? available.get(available.size() - 1) : available.get(0);
        }
        int next = Math.floorMod(current + (backwards ? -1 : 1), available.size());
        return available.get(next);
    }

    private static boolean intersects(AnchorLayout.Bounds first, AnchorLayout.Bounds second) {
        if (first == null || second == null || first.width() <= 0 || first.height() <= 0
                || second.width() <= 0 || second.height() <= 0) {
            return false;
        }
        return first.x() < (long) second.x() + second.width()
                && (long) first.x() + first.width() > second.x()
                && first.y() < (long) second.y() + second.height()
                && (long) first.y() + first.height() > second.y();
    }
}
