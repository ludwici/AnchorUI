package com.ludwici.anchorui.client.gui.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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

    public static List<String> sorted(Collection<String> ids, String currentNamespace) {
        List<String> result = new ArrayList<>(ids);
        result.sort(Comparator.comparingInt((String id) -> group(id, currentNamespace))
                .thenComparing(String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Comparator.naturalOrder()));
        return List.copyOf(result);
    }
}
