package com.potato.potatotool.content.redTeam.memshell.util;

import java.util.AbstractMap;
import java.util.Map;

/**
 * @author Potato
 * @date 2024/8/5 10:47
 */
public class RandomHeaderUtil {
    private static final String[] HEADER_KEYS = {"Referer", "User-Agent"};
    private static final String HEADER_TOKEN_CHARS = "!#$%&'*+-.^_`|~";

    // 生成一个随机的HTTP头部键值对
    public static Map.Entry<String, String> generateRandomHeader() {
        String key = getRandomHeaderKey();
        String value = generateRandomHeaderValue(key);
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    // 从预定义的HTTP头部键数组中随机选择一个键
    private static String getRandomHeaderKey() {
        return MemoryShellRandomUtil.randomChoice(HEADER_KEYS);
    }

    // 根据提供的键生成一个随机的值
    private static String generateRandomHeaderValue(String key) {
        switch (key) {
            case "Referer":
            case "User-Agent":
                return MemoryShellRandomUtil.randomAlpha(4, 10);
            default:
                return "";
        }
    }

    public static String requireValidHeaderName(String headerName) {
        String normalized = headerName == null ? null : headerName.trim();
        if (!isValidHeaderName(normalized)) {
            throw new IllegalArgumentException("Invalid HTTP header name: " + headerName);
        }
        return normalized;
    }

    public static String requireValidHeaderValue(String headerValue) {
        String normalized = headerValue == null ? null : headerValue.trim();
        if (!isValidHeaderValue(normalized)) {
            throw new IllegalArgumentException("Invalid HTTP header value: " + headerValue);
        }
        return normalized;
    }

    public static boolean isValidHeaderName(String headerName) {
        if (headerName == null || headerName.isEmpty()) {
            return false;
        }
        for (int i = 0; i < headerName.length(); i++) {
            char ch = headerName.charAt(i);
            if (!isHeaderTokenChar(ch)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidHeaderValue(String headerValue) {
        if (headerValue == null || headerValue.trim().isEmpty()) {
            return false;
        }
        for (int i = 0; i < headerValue.length(); i++) {
            char ch = headerValue.charAt(i);
            if (ch <= 0x1F || ch == 0x7F) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHeaderTokenChar(char ch) {
        return (ch >= 'A' && ch <= 'Z')
                || (ch >= 'a' && ch <= 'z')
                || (ch >= '0' && ch <= '9')
                || HEADER_TOKEN_CHARS.indexOf(ch) >= 0;
    }
}
