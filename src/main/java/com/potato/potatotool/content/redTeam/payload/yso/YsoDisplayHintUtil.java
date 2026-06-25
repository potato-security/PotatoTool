package com.potato.potatotool.content.redTeam.payload.yso;

import java.util.ArrayList;
import java.util.List;

public final class YsoDisplayHintUtil {
    private static final String JDK = "JDK";
    private static final String LOCAL_HELPER = "PotatoTool local helper";

    private YsoDisplayHintUtil() {
    }

    public static String formatDependencyHint(String hint) {
        String value = normalizeText(hint);
        if (value.isEmpty()) {
            return "Dependency: unknown";
        }
        if (JDK.equals(value)) {
            return "Dependency: JDK only";
        }
        if (LOCAL_HELPER.equals(value)) {
            return "Dependency: " + LOCAL_HELPER;
        }
        String[] items = value.split("\\s*,\\s*");
        List<String> formatted = new ArrayList<String>();
        for (int i = 0; i < items.length; i++) {
            String item = formatDependencyItem(items[i]);
            if (!item.isEmpty()) {
                formatted.add(item);
            }
        }
        if (formatted.isEmpty()) {
            return "Dependency: " + value;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Maven dependencies (").append(formatted.size()).append("): ");
        for (int i = 0; i < formatted.size(); i++) {
            if (i > 0) {
                builder.append("; ");
            }
            builder.append(formatted.get(i));
        }
        return builder.toString();
    }

    public static String formatVerificationHint(String hint) {
        String value = normalizeText(hint);
        if (value.isEmpty()) {
            return "Offline check: not specified";
        }
        if ("HashMap<URL, ?> object stream".equals(value)) {
            return "Offline check: serialized HashMap<URL, ?> object stream";
        }
        if ("object stream starts with TC_OBJECT".equals(value)) {
            return "Offline check: stream header plus TC_OBJECT root";
        }
        if ("offline generation only; target JRE constrained".equals(value)) {
            return "Offline check: generation only; target JRE compatibility is constrained";
        }
        if ("raw object stream, not serialized byte[]".equals(value)) {
            return "Offline check: raw object stream; no serialized byte[] wrapper";
        }
        if ("serializes selected class bytes into ClassFilePayload".equals(value)) {
            return "Offline check: selected class bytes serialize into ClassFilePayload";
        }
        return "Offline check: " + value;
    }

    private static String formatDependencyItem(String raw) {
        String value = normalizeText(raw);
        if (value.isEmpty()) {
            return "";
        }
        String[] parts = value.split(":");
        if (parts.length >= 3) {
            String artifact = parts[1].trim();
            String version = parts[parts.length - 1].trim();
            if (!artifact.isEmpty() && !version.isEmpty()) {
                return artifact + " " + version;
            }
        }
        return value;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
