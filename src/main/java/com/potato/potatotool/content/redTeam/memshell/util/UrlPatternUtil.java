package com.potato.potatotool.content.redTeam.memshell.util;

public class UrlPatternUtil {

    public static String requireValidUrlPattern(String urlPattern) {
        String normalized = normalizeUrlPattern(urlPattern);
        if (!isValidUrlPattern(normalized)) {
            throw new IllegalArgumentException("Invalid URL pattern: " + urlPattern);
        }
        return normalized;
    }

    public static boolean isValidUrlPattern(String urlPattern) {
        if (urlPattern == null || urlPattern.isEmpty() || !urlPattern.startsWith("/")) {
            return false;
        }
        for (int i = 0; i < urlPattern.length(); i++) {
            char ch = urlPattern.charAt(i);
            if (ch <= 0x20 || ch == 0x7F || ch == '\\') {
                return false;
            }
        }
        return true;
    }

    public static String normalizeUrlPattern(String urlPattern) {
        return urlPattern == null ? null : urlPattern.trim();
    }
}
