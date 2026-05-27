package com.potato.potatotool.content.redTeam.portScanner.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HostInputParser {
    private HostInputParser() {
    }

    public static List<String> parseLines(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] lines = input.split("\\r?\\n");
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String value = stripComment(line).trim();
            if (!value.isEmpty()) {
                result.add(normalize(value));
            }
        }
        return result;
    }

    private static String normalize(String value) {
        String normalized = value.trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            try {
                URI uri = URI.create(normalized);
                if (uri.getHost() != null && !uri.getHost().trim().isEmpty()) {
                    return uri.getHost();
                }
            } catch (Exception ignored) {
                throw new IllegalArgumentException("Invalid target: " + value);
            }
        }
        if (normalized.contains("://")) {
            throw new IllegalArgumentException("Invalid target: " + value);
        }
        return normalized;
    }

    private static String stripComment(String line) {
        if (line == null) {
            return "";
        }
        int index = line.indexOf('#');
        if (index >= 0) {
            return line.substring(0, index);
        }
        return line;
    }
}
