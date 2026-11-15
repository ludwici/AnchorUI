package com.ludwici.anchorui.client.gui.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Set;

public final class EditorWidgetOrder {
    private EditorWidgetOrder() {
    }

    public static int group(String id, String currentNamespace) {
        String namespace = namespace(id);
        if ("minecraft".equals(namespace)) {
            return 0;
        }
        if (namespace.equals(currentNamespace)) {
            return 1;
        }
        return 2;
    }

    public static String namespace(String id) {
        int separator = id.indexOf(':');
        return separator > 0 ? id.substring(0, separator).toLowerCase(Locale.ROOT) : "";
    }

    public record Node(String id, int depth, boolean hasChildren) {
    }

    public static List<Node> hierarchy(Collection<String> ids, String currentNamespace,
                                       Map<String, String> parents, Set<String> collapsed) {
        List<String> ordered = sorted(ids, currentNamespace);
        Set<String> available = new HashSet<>(ordered);
        Map<String, List<String>> children = new LinkedHashMap<>();
        for (String id : ordered) {
            String parent = parents.get(id);
            if (parent != null && available.contains(parent)
                    && group(id, currentNamespace) == group(parent, currentNamespace)) {
                children.computeIfAbsent(parent, ignored -> new ArrayList<>()).add(id);
            }
        }
        List<Node> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : ordered) {
            String parent = parents.get(id);
            if (parent == null || !available.contains(parent)
                    || group(id, currentNamespace) != group(parent, currentNamespace)) {
                append(result, id, 0, children, collapsed, seen);
            }
        }
        for (String id : ordered) {
            if (!seen.contains(id)) {
                append(result, id, 0, children, collapsed, seen);
            }
        }
        return List.copyOf(result);
    }

    private static void append(List<Node> result, String id, int depth,
                               Map<String, List<String>> children, Set<String> collapsed, Set<String> seen) {
        if (!seen.add(id)) {
            return;
        }
        List<String> descendants = children.getOrDefault(id, List.of());
        result.add(new Node(id, depth, !descendants.isEmpty()));
        if (collapsed.contains(id)) {
            markDescendants(descendants, children, seen);
            return;
        }
        for (String child : descendants) {
            append(result, child, Math.min(depth + 1, 12), children, collapsed, seen);
        }
    }

    private static void markDescendants(List<String> descendants, Map<String, List<String>> children, Set<String> seen) {
        for (String child : descendants) {
            if (seen.add(child)) {
                markDescendants(children.getOrDefault(child, List.of()), children, seen);
            }
        }
    }

    public static List<String> sorted(Collection<String> ids, String currentNamespace) {
        List<String> result = new ArrayList<>(ids);
        result.sort(Comparator.comparingInt((String id) -> group(id, currentNamespace)).thenComparing(String.CASE_INSENSITIVE_ORDER).thenComparing(Comparator.naturalOrder()));
        return List.copyOf(result);
    }
}
