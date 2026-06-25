package com.potato.potatotool.content.redTeam.memshell.util;

import java.security.SecureRandom;

public final class MemoryShellRandomUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHA_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER_ALPHA_CHARS = "abcdefghijklmnopqrstuvwxyz";

    private MemoryShellRandomUtil() {
    }

    public static String randomAlpha(int minLength, int maxLength) {
        return randomString(ALPHA_CHARS, minLength, maxLength);
    }

    public static String randomLowerAlpha(int length) {
        return randomString(LOWER_ALPHA_CHARS, length, length);
    }

    public static String randomChoice(String[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Random choices are empty");
        }
        return values[SECURE_RANDOM.nextInt(values.length)];
    }

    private static String randomString(String alphabet, int minLength, int maxLength) {
        if (alphabet == null || alphabet.isEmpty()) {
            throw new IllegalArgumentException("Random alphabet is empty");
        }
        if (minLength < 0 || maxLength < minLength) {
            throw new IllegalArgumentException("Invalid length parameters");
        }
        int length = minLength + SECURE_RANDOM.nextInt(maxLength - minLength + 1);
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
