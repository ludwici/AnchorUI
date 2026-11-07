package com.ludwici.anchorui.api;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoreParser {
    private static class Tag {
        String name;
        Map<String, Object> attrs;

        Tag(String name, Map<String, Object> attrs) {
            this.name = name;
            this.attrs = attrs;
        }
    }

    public static List<LorePiece> parse(String input) {
        List<LorePiece> result = new ArrayList<>();
        Deque<Tag> tagStack = new ArrayDeque<>();

        int i = 0;
        StringBuilder currentText = new StringBuilder();

        while (i < input.length()) {
            if (input.charAt(i) == '<') {
                if (currentText.length() > 0) {
                    result.add(new LorePiece(currentText.toString(), mergeTags(tagStack)));
                    currentText.setLength(0);
                }

                int closeIndex = input.indexOf('>', i);
                if (closeIndex == -1) {
                    break;
                }

                String tagContent = input.substring(i + 1, closeIndex).trim();

                if (tagContent.startsWith("/")) {
                    String tagName = tagContent.substring(1).trim();
                    if (!tagStack.isEmpty() && tagStack.peek().name.equals(tagName)) {
                        tagStack.pop();
                    } else {
                        System.err.println("⚠ Unexpected closing tag: " + tagName);
                    }
                } else {
                    Tag tag = parseTag(tagContent);
                    tagStack.push(tag);
                }
                i = closeIndex + 1;
            } else {
                currentText.append(input.charAt(i));
                i++;
            }
        }

        if (currentText.length() > 0) {
            result.add(new LorePiece(currentText.toString(), mergeTags(tagStack)));
        }

        return result;
    }

    private static Tag parseTag(String tagContent) {
        tagContent = tagContent.trim();
        String[] parts = tagContent.split("\\s+", 2);
        String name = parts[0];
        Map<String, Object> attrs = new LinkedHashMap<>();
        if (parts.length > 1) {
            Matcher attrMatcher = Pattern.compile("(\\w+)\\s*=\\s*\"([^\"]*)\"").matcher(parts[1]);
            while (attrMatcher.find()) {
                attrs.put(attrMatcher.group(1), attrMatcher.group(2));
            }
        }

        return new Tag(name, attrs);
    }

    private static Map<String, Object> mergeTags(Deque<Tag> tagStack) {
        Map<String, Object> merged = new LinkedHashMap<>();
        for (Tag tag : tagStack) {
            switch (tag.name) {
                case "b" -> merged.put("bold", true);
                case "i" -> merged.put("italic", true);
                case "u" -> merged.put("underlined", true);
                default -> {
                    if (!tag.attrs.isEmpty()) {
                        merged.put(tag.name, new LinkedHashMap<>(tag.attrs));
                    }
                }
            }
        }

        return merged;
    }
}
