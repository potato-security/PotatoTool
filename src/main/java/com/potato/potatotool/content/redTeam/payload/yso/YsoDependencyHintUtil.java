package com.potato.potatotool.content.redTeam.payload.yso;

import com.potato.potatotool.content.redTeam.payload.yso.thirdparty.woodpecker.payloads.annotation.Dependencies;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class YsoDependencyHintUtil {
    private YsoDependencyHintUtil() {
    }

    public static String summarize(Class<?> payloadClass) {
        if (payloadClass == null) {
            return "";
        }
        String[] values = Dependencies.Utils.getDependencies(payloadClass);
        if (values == null || values.length == 0) {
            return "JDK";
        }
        Set<String> normalized = new LinkedHashSet<String>();
        for (int i = 0; i < values.length; i++) {
            String value = normalize(values[i]);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        if (normalized.isEmpty()) {
            return "JDK";
        }
        List<String> ordered = new ArrayList<String>(normalized);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ordered.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(ordered.get(i));
        }
        return builder.toString();
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty() || value.startsWith("//")) {
            return "";
        }
        String[] parts = value.split(":");
        if (parts.length >= 3) {
            return parts[0] + ":" + parts[1] + ":" + parts[parts.length - 1];
        }
        return value;
    }
}
