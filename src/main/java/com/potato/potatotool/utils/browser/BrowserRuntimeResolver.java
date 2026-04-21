package com.potato.potatotool.utils.browser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves locally installed Chromium-family browsers for features that need a browser runtime.
 */
public final class BrowserRuntimeResolver {
    private static final List<String> MAC_BROWSER_PATHS = Arrays.asList(
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "/Applications/Chromium.app/Contents/MacOS/Chromium",
            "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"
    );
    private static final List<String> LINUX_BROWSER_PATHS = Arrays.asList(
            "/usr/bin/google-chrome",
            "/usr/bin/google-chrome-stable",
            "/usr/bin/chromium",
            "/usr/bin/chromium-browser",
            "/snap/bin/chromium"
    );
    private static final List<String> WINDOWS_BROWSER_PATHS = Arrays.asList(
            "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
            "C:\\Program Files\\Chromium\\Application\\chrome.exe"
    );

    private BrowserRuntimeResolver() {
    }

    public static String resolveBrowserPath(String configuredBrowserPath) {
        String configuredPath = resolveConfiguredBrowserPath(configuredBrowserPath);
        if (configuredPath != null) {
            return configuredPath;
        }
        return autoDetectBrowserPath();
    }

    public static String resolveConfiguredBrowserPath(String configuredBrowserPath) {
        if (configuredBrowserPath == null || configuredBrowserPath.trim().isEmpty()) {
            return null;
        }
        String trimmed = configuredBrowserPath.trim();
        File configured = new File(trimmed);
        if (configured.exists() && configured.isFile()) {
            return configured.getAbsolutePath();
        }
        return null;
    }

    public static String autoDetectBrowserPath() {
        for (String candidate : getOsBrowserCandidates()) {
            File file = new File(candidate);
            if (file.exists() && file.isFile()) {
                return file.getAbsolutePath();
            }
        }

        List<String> commandCandidates = Arrays.asList("google-chrome", "google-chrome-stable", "chromium", "chromium-browser", "chrome");
        for (String command : commandCandidates) {
            String path = findCommandPath(command);
            if (path != null) {
                return path;
            }
        }

        return null;
    }

    public static String readCommandVersion(String executablePath, String versionArg) {
        if (executablePath == null || executablePath.trim().isEmpty()) {
            return null;
        }
        List<String> command = new ArrayList<String>();
        command.add(executablePath);
        command.add(versionArg);
        return runProcessAndReadLine(command, 5);
    }

    public static Integer parseMajorVersion(String versionText) {
        if (versionText == null || versionText.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = Pattern.compile("(\\d+)\\.").matcher(versionText);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }

        return null;
    }

    private static List<String> getOsBrowserCandidates() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            return WINDOWS_BROWSER_PATHS;
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return MAC_BROWSER_PATHS;
        }
        return LINUX_BROWSER_PATHS;
    }

    public static String findCommandPath(String command) {
        String osName = System.getProperty("os.name", "").toLowerCase();
        List<String> locateCommand = osName.contains("win")
                ? Arrays.asList("where", command)
                : Arrays.asList("which", command);
        String output = runProcessAndReadLine(locateCommand, 4);
        if (output == null || output.trim().isEmpty()) {
            return null;
        }
        String path = output.trim();
        File file = new File(path);
        return file.exists() ? file.getAbsolutePath() : null;
    }

    private static String runProcessAndReadLine(List<String> command, int timeoutSeconds) {
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            process = builder.start();
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                line = reader.readLine();
            }
            process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            return line;
        } catch (Exception e) {
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
