package com.ludwici.anchorui.api.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextTemplateParts {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-z0-9_.-]+:[a-z0-9_./-]+)(?:#([a-zA-Z0-9_.\\[\\]-]+))?\\}\\}");

    public record Part(String text, String source, String path) {
        public boolean dynamic() {
            return source != null;
        }
    }

    private TextTemplateParts() {
    }

    public static List<Part> parse(String template) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        List<Part> result = new ArrayList<>();
        int start = 0;
        while (matcher.find()) {
            if (matcher.start() > start) {
                result.add(new Part(template.substring(start, matcher.start()), null, ""));
            }
            String path = matcher.group(2) == null ? "" : matcher.group(2);
            DataValuePaths.validate(path);
            result.add(new Part("", matcher.group(1), path));
            start = matcher.end();
            if (result.size() > 128) {
                throw new IllegalArgumentException("Too many text binding placeholders");
            }
        }
        if (start < template.length() || result.isEmpty()) {
            result.add(new Part(template.substring(start), null, ""));
        }
        return List.copyOf(result);
    }
}
