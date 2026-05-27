package com.potato.potatotool.content.redTeam.vulnScanner.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regex compatibility helpers for POC formats that accept literal braces in regex strings.
 */
public final class RegexCompat {

    private RegexCompat() {
    }

    public static Pattern compile(String regex) {
        return compile(regex, 0);
    }

    public static Pattern compile(String regex, int flags) {
        try {
            return Pattern.compile(regex, flags);
        } catch (PatternSyntaxException first) {
            String normalized = escapeLiteralBraces(regex);
            if (normalized.equals(regex)) {
                throw first;
            }
            try {
                return Pattern.compile(normalized, flags);
            } catch (PatternSyntaxException ignored) {
                throw first;
            }
        }
    }

    private static String escapeLiteralBraces(String regex) {
        if (regex == null || regex.isEmpty()) {
            return regex;
        }

        StringBuilder result = new StringBuilder(regex.length() + 8);
        boolean inCharClass = false;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '\\') {
                result.append(c);
                if (i + 1 < regex.length()) {
                    result.append(regex.charAt(++i));
                }
                continue;
            }
            if (c == '[') {
                inCharClass = true;
                result.append(c);
                continue;
            }
            if (c == ']') {
                inCharClass = false;
                result.append(c);
                continue;
            }
            if (!inCharClass && c == '{' && !isValidQuantifier(regex, i)) {
                result.append("\\{");
                continue;
            }
            if (!inCharClass && c == '}' && !hasQuantifierOpenBefore(regex, i)) {
                result.append("\\}");
                continue;
            }
            result.append(c);
        }
        return result.toString();
    }

    private static boolean isValidQuantifier(String regex, int openIndex) {
        int i = openIndex + 1;
        if (i >= regex.length() || !Character.isDigit(regex.charAt(i))) {
            return false;
        }
        while (i < regex.length() && Character.isDigit(regex.charAt(i))) {
            i++;
        }
        if (i < regex.length() && regex.charAt(i) == ',') {
            i++;
            while (i < regex.length() && Character.isDigit(regex.charAt(i))) {
                i++;
            }
        }
        return i < regex.length() && regex.charAt(i) == '}';
    }

    private static boolean hasQuantifierOpenBefore(String regex, int closeIndex) {
        for (int i = closeIndex - 1; i >= 0; i--) {
            char c = regex.charAt(i);
            if (c == '\\') {
                i--;
                continue;
            }
            if (c == '{') {
                return isValidQuantifier(regex, i);
            }
            if (!Character.isDigit(c) && c != ',') {
                return false;
            }
        }
        return false;
    }
}
