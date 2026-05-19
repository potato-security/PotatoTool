package com.potato.potatotool.controller.redTeam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class PaneVulScanTargetSupport {

    private PaneVulScanTargetSupport() {
    }

    static List<String> parseTargets(String targetText) {
        if (targetText == null) {
            return Collections.emptyList();
        }
        String trimmed = targetText.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(trimmed.split("\n"))
                .map(String::trim)
                .filter(value -> !value.isEmpty() && !value.startsWith("#"))
                .collect(Collectors.toList());
    }

    static List<String> readTargetLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    static String readTargetFileContent(Path path) throws IOException {
        return String.join("\n", readTargetLines(path));
    }
}
