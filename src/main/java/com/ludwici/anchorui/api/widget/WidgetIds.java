package com.ludwici.anchorui.api.widget;

import java.util.regex.Pattern;

public final class WidgetIds {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_.\\-/]+");

    private WidgetIds() {
    }

    public static String id(String value, String defaultNamespace) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Widget id must not be empty");
        }
        if (value.equals("screen")) {
            throw new IllegalArgumentException("screen is a reserved widget id");
        }
        String namespace = defaultNamespace;
        String path = value;
        int separator = value.indexOf(':');
        if (separator >= 0) {
            namespace = value.substring(0, separator);
            path = value.substring(separator + 1);
        }
        if (path.endsWith("/__content")) {
            throw new IllegalArgumentException("Unsupported internal widget ID: " + value + "; use the panel ID instead");
        }
        if (namespace == null || !NAMESPACE.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid widget id: " + value);
        }
        return namespace + ":" + path;
    }

    public static String reference(String value, String defaultNamespace) {
        if (value == null || value.isBlank() || value.equals("screen")) {
            return "screen";
        }
        return id(value, defaultNamespace);
    }

    public static String namespace(String id) {
        int separator = id.indexOf(':');
        if (separator < 1) {
            throw new IllegalArgumentException("Expected qualified widget id: " + id);
        }
        return id.substring(0, separator);
    }
}
